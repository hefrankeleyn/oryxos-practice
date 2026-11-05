package com.oryxlabs.oryxos.provider;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.openai.OpenAiChatOptions;

class ProviderSelectionTest {
  static ProviderRequest request(String name) {
    return new ProviderRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        1,
        new ProfileSnapshot("agent", name, "model", null, List.of(), List.of(), 10, 20, ""),
        "system",
        List.of(),
        List.of());
  }

  @Test
  void explicitInstancesAreSelectedWithoutTypeScanning() {
    var a = new AtomicInteger();
    var b = new AtomicInteger();
    ChatModel first = target(a, "first"), second = target(b, "second");
    assertThat(first.getClass()).isEqualTo(second.getClass());
    var service = new SpringAiProviderService(Map.of("first", first, "second", second));
    assertThat(service.call(request("first")).text()).isEqualTo("first");
    assertThat(b.get()).isZero();
    assertThat(service.call(request("second")).text()).isEqualTo("second");
    assertThat(a.get()).isEqualTo(1);
    assertThatThrownBy(() -> service.call(request("unknown"))).isInstanceOf(RuntimeFailure.class);
    assertThat(a.get() + b.get()).isEqualTo(2);
  }

  @Test
  void failureDoesNotRetainRawCauseOrResponse() {
    ChatModel bad =
        p -> {
          throw new IllegalStateException("Authorization fixture-secret");
        };
    assertThatThrownBy(
            () -> new SpringAiProviderService(Map.of("first", bad)).call(request("first")))
        .isInstanceOf(RuntimeFailure.class)
        .hasMessage("PROVIDER")
        .hasNoCause();
  }

  @Test
  void officialMissingUsageFieldsAreNotInventedAsZeroOrSum() {
    var raw = new org.springframework.ai.openai.api.OpenAiApi.Usage(0, null, null);
    var usage = new org.springframework.ai.chat.metadata.DefaultUsage(0, 0, 0, raw);
    ChatModel model =
        p ->
            new ChatResponse(
                List.of(new Generation(new AssistantMessage("ok"))),
                org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                    .usage(usage)
                    .build());
    var response = new SpringAiProviderService(Map.of("first", model)).call(request("first"));
    assertThat(response.promptTokens()).isNull();
    assertThat(response.completionTokens()).isZero();
    assertThat(response.totalTokens()).isNull();
  }

  @Test
  void normalLookingResponseCannotEchoKnownCredentials() {
    String key = "fixture-private-key";
    ChatModel model =
        p -> new ChatResponse(List.of(new Generation(new AssistantMessage("answer " + key))));
    assertThatThrownBy(
            () ->
                new SpringAiProviderService(Map.of("first", model), List.of(key))
                    .call(request("first")))
        .isInstanceOf(RuntimeFailure.class)
        .hasMessage("PROTOCOL")
        .hasNoCause();
  }

  private ChatModel target(AtomicInteger count, String answer) {
    return p -> {
      count.incrementAndGet();
      assertThat(((OpenAiChatOptions) p.getOptions()).getInternalToolExecutionEnabled()).isFalse();
      return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
    };
  }
}
