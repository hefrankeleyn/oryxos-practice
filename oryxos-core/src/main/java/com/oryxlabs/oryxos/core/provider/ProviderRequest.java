package com.oryxlabs.oryxos.core.provider;

import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.tool.OryxTool;
import java.util.*;

/** 一次已分配调用身份的请求；系统指令及历史均不允许进入审计正文。 */
public record ProviderRequest(
    UUID callId,
    UUID sessionId,
    UUID runId,
    int iteration,
    ProfileSnapshot profile,
    String system,
    List<Message> messages,
    List<OryxTool> tools) {
  public ProviderRequest {
    messages = List.copyOf(messages);
    tools = List.copyOf(tools);
  }

  @Override
  public String toString() {
    return "ProviderRequest[redacted]";
  }
}
