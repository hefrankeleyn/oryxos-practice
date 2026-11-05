package com.oryxlabs.oryxos.provider;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import com.oryxlabs.oryxos.core.tool.OryxTool;
import org.junit.jupiter.api.Test;

class ToolSchemaAdapterTest {
  @Test
  void callbackIsMetadataOnly() {
    OryxTool tool =
        new OryxTool() {
          public String name() {
            return "calculate";
          }

          public String description() {
            return "fixture";
          }

          public String inputSchema() {
            return "{\"type\":\"object\",\"properties\":{}}";
          }

          public String execute(String a) {
            throw new AssertionError("forbidden automatic execution");
          }
        };
    var callback = new ToolSchemaAdapter().adapt(tool);
    assertThat(callback.getToolDefinition().name()).isEqualTo("calculate");
    assertThatThrownBy(() -> callback.call("{}")).isInstanceOf(RuntimeFailure.class);
  }

  @Test
  void repeatedIdsInOneModelResponseAreProtocolFailure() {
    var count = new java.util.concurrent.atomic.AtomicInteger();
    org.springframework.ai.chat.model.ChatModel model =
        p -> {
          count.incrementAndGet();
          return new org.springframework.ai.chat.model.ChatResponse(
              java.util.List.of(
                  new org.springframework.ai.chat.model.Generation(
                      new org.springframework.ai.chat.messages.AssistantMessage(
                          "",
                          java.util.Map.of(),
                          java.util.List.of(
                              new org.springframework.ai.chat.messages.AssistantMessage.ToolCall(
                                  "same", "function", "calculate", "{}"),
                              new org.springframework.ai.chat.messages.AssistantMessage.ToolCall(
                                  "same", "function", "calculate", "{}"))))));
        };
    assertThatThrownBy(
            () ->
                new SpringAiProviderService(java.util.Map.of("first", model))
                    .call(ProviderSelectionTest.request("first")))
        .isInstanceOf(RuntimeFailure.class)
        .hasMessage("PROTOCOL");
    assertThat(count.get()).isEqualTo(1);
  }
}
