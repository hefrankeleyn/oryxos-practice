package com.oryxlabs.oryxos;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class AuditCoverageIntegrationTest {
  @TempDir Path home;

  @Test
  void allFiveRequiredPathsHavePersistentCorrelatedEvidence() throws Exception {
    var count = new AtomicInteger();
    try (var h =
        new TestRuntimeHarness(
            home,
            10,
            r -> {
              int step = count.getAndIncrement();
              return switch (step) {
                case 0 ->
                    TestRuntimeHarness.tools(
                        TestRuntimeHarness.add("success"),
                        new ProviderResponse.ToolCall("fail", "fail_action", "{}"),
                        new ProviderResponse.ToolCall("deny", "denied_action", "{}"));
                case 1 -> TestRuntimeHarness.done();
                default -> throw new RuntimeFailure(RuntimeFailure.Code.PROVIDER);
              };
            })) {
      h.agent.process(h.session, "tools");
      assertThatThrownBy(() -> h.agent.process(h.session, "model failure"))
          .isInstanceOf(RuntimeFailure.class);
      assertThat(
              h.storage.jdbc.queryForList(
                  "select status from llm_calls order by rowid", String.class))
          .containsExactly("SUCCEEDED", "SUCCEEDED", "FAILED");
      assertThat(
              h.storage.jdbc.queryForList(
                  "select status from tool_invocations order by rowid", String.class))
          .containsExactly("SUCCEEDED", "FAILED", "REJECTED");
      assertThat(
              h.storage.jdbc.queryForList(
                  "select executed from tool_invocations order by rowid", Integer.class))
          .containsExactly(1, 1, 0);
      assertThat(
              h.storage.jdbc.queryForObject(
                  "select count(*) from tool_invocations t join llm_calls l on t.llm_call_id=l.id and t.session_id=l.session_id and t.run_id=l.run_id and t.iteration=l.iteration",
                  Integer.class))
          .isEqualTo(3);
      assertThat(h.calculate.executions.get()).isEqualTo(1);
      assertThat(h.denied.executions.get()).isZero();
    }
  }
}
