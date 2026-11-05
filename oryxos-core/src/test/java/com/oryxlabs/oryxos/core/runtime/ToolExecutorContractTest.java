package com.oryxlabs.oryxos.core.runtime;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.tool.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolExecutorContractTest {
  static final AuditStore OK =
      new AuditStore() {
        public void begin(InvocationAudit a) {}

        public void complete(InvocationAudit a, InvocationAudit.Completion c) {}
      };

  InvocationAudit invocation(UUID run, UUID llm) {
    return new InvocationAudit(
        UUID.randomUUID(),
        InvocationAudit.Kind.TOOL,
        UUID.randomUUID(),
        "fixture",
        run,
        1,
        10,
        Instant.now(),
        null,
        null,
        llm,
        "wire-id",
        "calculate");
  }

  @Test
  void duplicateDispatchReusesReceiptButNewModelResponseIsIndependent() {
    var count = new AtomicInteger();
    ToolInvoker invoker =
        (n, a, allowed) -> {
          count.incrementAndGet();
          return new ToolResult(true, true, "2", null, false);
        };
    var executor = new ToolExecutor(invoker, OK, new ExecutionReceiptStore());
    UUID run = UUID.randomUUID(), llm = UUID.randomUUID();
    var a = invocation(run, llm);
    assertThat(executor.execute(a, "{}", List.of("calculate")))
        .isEqualTo(executor.execute(a, "{}", List.of("calculate")));
    assertThat(count.get()).isEqualTo(1);
    executor.execute(invocation(run, UUID.randomUUID()), "{}", List.of("calculate"));
    assertThat(count.get()).isEqualTo(2);
  }

  @Test
  void claimedOrUncertainRequestNeverReplays() {
    var receipts = new ExecutionReceiptStore();
    UUID run = UUID.randomUUID(), llm = UUID.randomUUID();
    var a = invocation(run, llm);
    receipts.claim(new ToolRequestIdentity(run, llm, "wire-id"));
    var count = new AtomicInteger();
    assertThatThrownBy(
            () ->
                new ToolExecutor(
                        (n, arg, allowed) -> {
                          count.incrementAndGet();
                          return null;
                        },
                        OK,
                        receipts)
                    .execute(a, "{}", List.of()))
        .isInstanceOf(AuditPersistenceException.class);
    assertThat(count.get()).isZero();
  }

  @Test
  void auditFailureIsFatalAndReceiptCannotHideIt() {
    var count = new AtomicInteger();
    var receipts = new ExecutionReceiptStore();
    var a = invocation(UUID.randomUUID(), UUID.randomUUID());
    AuditStore broken =
        new AuditStore() {
          public void begin(InvocationAudit x) {}

          public void complete(InvocationAudit x, InvocationAudit.Completion c) {
            throw new AuditPersistenceException();
          }
        };
    var executor =
        new ToolExecutor(
            (n, arg, allowed) -> {
              count.incrementAndGet();
              return new ToolResult(true, true, "2", null, false);
            },
            broken,
            receipts);
    assertThatThrownBy(() -> executor.execute(a, "{}", List.of("calculate")))
        .isInstanceOf(AuditPersistenceException.class);
    assertThatThrownBy(() -> executor.execute(a, "{}", List.of("calculate")))
        .isInstanceOf(AuditPersistenceException.class);
    assertThat(count.get()).isEqualTo(1);
  }
}
