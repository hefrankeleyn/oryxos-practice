package com.oryxlabs.oryxos;

import static org.junit.jupiter.api.Assertions.*;

import com.oryxlabs.oryxos.core.provider.ProviderDefinition;
import com.oryxlabs.oryxos.provider.*;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** 真实供应商验收只允许显式启用；默认跳过绝不计作验收成功。 */
@EnabledIfSystemProperty(named = "oryxos.acceptance.deepseek", matches = "true")
class DeepSeekAcceptanceTest {
  @TempDir Path home;

  @Test
  void realConversationToolResultAndDurableAudit() throws Exception {
    String key = System.getenv("DEEPSEEK_API_KEY"), model = System.getenv("DEEPSEEK_MODEL");
    assertTrue(
        key != null && !key.isBlank(), "DEEPSEEK_API_KEY is required when acceptance is enabled");
    assertTrue(
        model != null && !model.isBlank(), "DEEPSEEK_MODEL is required when acceptance is enabled");
    var definition =
        new ProviderDefinition(
            "deepseek",
            URI.create("https://api.deepseek.com"),
            key,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60));
    var target =
        new SpringAiProviderService(
            new ProviderModelConfiguration().create(Map.of("deepseek", definition)), List.of(key));
    try (var h = new TestRuntimeHarness(home, 10, target, a -> a, "deepseek", model)) {
      h.agent.process(h.session, "记住数字 7，暂时不要调用工具。");
      String recall = h.agent.process(h.session, "我刚才让你记住哪个数字？不要调用工具。");
      assertTrue(recall.contains("7"), "conversation context must be retained");
      String result = h.agent.process(h.session, "请调用 calculate 工具计算 1+2，只调用一次，然后直接给出结果。");
      assertTrue(result.contains("3"), "final answer must use tool result");
      assertEquals(1, h.calculate.executions.get(), "one actual tool execution required");
      assertEquals(
          1, h.storage.jdbc.queryForObject("select count(*) from tool_invocations", Integer.class));
      assertEquals(
          1,
          h.storage.jdbc.queryForObject(
              "select count(*) from tool_invocations where status='SUCCEEDED' and executed=1",
              Integer.class));
      assertTrue(
          h.storage.jdbc.queryForObject(
                  "select count(*) from llm_calls where status='SUCCEEDED'", Integer.class)
              >= 4);
      assertEquals(
          1,
          h.storage.jdbc.queryForObject(
              "select count(*) from tool_invocations t join llm_calls l on t.llm_call_id=l.id where l.run_id=t.run_id",
              Integer.class));
    }
  }
}
