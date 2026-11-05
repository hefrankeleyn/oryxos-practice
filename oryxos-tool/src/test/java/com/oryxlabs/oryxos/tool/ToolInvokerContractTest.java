package com.oryxlabs.oryxos.tool;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import com.oryxlabs.oryxos.core.tool.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToolInvokerContractTest {
  @Test
  void permissionAndSchemaFailuresNeverExecuteAndExecutionFailureIsSafe() {
    var count = new AtomicInteger();
    OryxTool tool =
        new OryxTool() {
          public String name() {
            return "fail";
          }

          public String description() {
            return "fixture";
          }

          public String inputSchema() {
            return "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"integer\"}},\"required\":[\"a\"],\"additionalProperties\":false}";
          }

          public String execute(String args) {
            count.incrementAndGet();
            throw new IllegalStateException("fixture-secret");
          }
        };
    var invoker =
        new DefaultToolInvoker(
            new ToolRegistry(List.of(tool)),
            a -> {
              throw new AssertionError("pure computation has no IO action");
            },
            new ToolInputValidator());
    assertThat(invoker.invoke("fail", "{\"a\":1}", List.of()).executed()).isFalse();
    for (String arg : List.of("{}", "{\"a\":\"x\"}", "{\"a\":1,\"extra\":2}", "{\"a\":1,\"a\":2}"))
      assertThat(invoker.invoke("fail", arg, List.of("fail")).executed()).isFalse();
    assertThat(count.get()).isZero();
    var result = invoker.invoke("fail", "{\"a\":1}", List.of("fail"));
    assertThat(result.executed()).isTrue();
    assertThat(result.success()).isFalse();
    assertThat(result.error()).isEqualTo(RuntimeFailure.Code.TOOL_EXECUTION);
    assertThat(result.content()).doesNotContain("fixture-secret");
    assertThat(count.get()).isEqualTo(1);
  }
}
