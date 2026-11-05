package com.oryxlabs.oryxos.core.model;

import com.oryxlabs.oryxos.core.provider.ProviderResponse;
import java.util.*;

/** 原始链消息；TOOL 的请求 ID 关联原 assistant，系统上下文另行构建。 */
public record Message(
    UUID id,
    UUID turnId,
    int sequence,
    Role role,
    String content,
    UUID llmCallId,
    List<ProviderResponse.ToolCall> toolCalls,
    String toolCallId,
    String toolName) {
  public enum Role {
    USER,
    ASSISTANT,
    TOOL
  }

  public Message {
    toolCalls = List.copyOf(toolCalls);
    content = content == null ? "" : content;
  }

  public static Message user(Turn turn, String text) {
    return new Message(
        UUID.randomUUID(),
        turn.id(),
        turn.messages().size(),
        Role.USER,
        text,
        null,
        List.of(),
        null,
        null);
  }

  public static Message assistant(Turn turn, UUID call, ProviderResponse response) {
    return new Message(
        UUID.randomUUID(),
        turn.id(),
        turn.messages().size(),
        Role.ASSISTANT,
        response.text(),
        call,
        response.toolCalls(),
        null,
        null);
  }

  @Override
  public String toString() {
    return "Message[redacted]";
  }
}
