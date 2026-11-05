package com.oryxlabs.oryxos;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.agent.ProfileContext;
import com.oryxlabs.oryxos.core.audit.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class AuditFailureStopsLoopTest {
  @TempDir Path home;

  @Test
  void failuresBeforeAndAfterEachKindStopTheRemainingLoop() throws Exception {
    for (var kind : InvocationAudit.Kind.values())
      for (boolean begin : new boolean[] {true, false}) {
        var calls = new AtomicInteger();
        try (var h =
            new TestRuntimeHarness(
                home.resolve(kind + "-" + begin),
                10,
                r -> {
                  calls.incrementAndGet();
                  return TestRuntimeHarness.tools(TestRuntimeHarness.add("id"));
                },
                real ->
                    new AuditStore() {
                      public void begin(InvocationAudit a) {
                        if (a.kind() == kind && begin) throw new AuditPersistenceException();
                        real.begin(a);
                      }

                      public void complete(InvocationAudit a, InvocationAudit.Completion c) {
                        if (a.kind() == kind && !begin) throw new AuditPersistenceException();
                        real.complete(a, c);
                      }
                    })) {
          assertThatThrownBy(() -> h.agent.process(h.session, "request"))
              .isInstanceOf(AuditPersistenceException.class);
          assertThat(calls.get()).isEqualTo(kind == InvocationAudit.Kind.LLM && begin ? 0 : 1);
          assertThat(h.calculate.executions.get())
              .isEqualTo(kind == InvocationAudit.Kind.TOOL && !begin ? 1 : 0);
          assertThat(ProfileContext.get()).isNull();
          if (!begin) {
            String table = kind == InvocationAudit.Kind.LLM ? "llm_calls" : "tool_invocations";
            assertThat(h.storage.jdbc.queryForObject("select status from " + table, String.class))
                .isEqualTo("PENDING");
          }
        }
      }
  }
}
