package com.oryxlabs.oryxos.core.runtime;

import com.oryxlabs.oryxos.core.audit.InvocationAudit;
import com.oryxlabs.oryxos.core.memory.MemoryService;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.provider.ProviderService;
import java.time.Instant;
import java.util.*;

/** 自主管理同步循环；模型单次调用、工具顺序调度，完整当前链不被历史投影截断。 */
public final class ReActLoop {
  private final ProviderService provider;
  private final PromptBuilder prompt;
  private final MemoryService memory;
  private final ToolExecutor tools;

  public ReActLoop(ProviderService provider, PromptBuilder prompt, MemoryService memory) {
    this(provider, prompt, memory, null);
  }

  public ReActLoop(
      ProviderService provider, PromptBuilder prompt, MemoryService memory, ToolExecutor tools) {
    this.provider = provider;
    this.prompt = prompt;
    this.memory = memory;
    this.tools = tools;
  }

  public String process(Session session, Turn turn, AgentDefinition agent) {
    for (int iteration = 1; iteration <= agent.profile().maxIterations(); iteration++) {
      var request = prompt.build(session, turn, agent, iteration);
      var response = provider.call(request);
      memory.append(turn, Message.assistant(turn, request.callId(), response));
      if (response.toolCalls().isEmpty()) {
        if (response.text().isBlank()) throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
        memory.finish(turn, Turn.State.SUCCEEDED);
        return response.text();
      }
      Set<String> ids = new HashSet<>();
      for (var c : response.toolCalls())
        if (c.id() == null
            || c.id().isBlank()
            || c.name() == null
            || c.name().isBlank()
            || !ids.add(c.id())) throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
      if (iteration == agent.profile().maxIterations())
        throw new RuntimeFailure(RuntimeFailure.Code.ITERATION_LIMIT);
      if (tools == null) throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
      for (var call : response.toolCalls()) {
        var audit =
            new InvocationAudit(
                UUID.randomUUID(),
                InvocationAudit.Kind.TOOL,
                session.id(),
                agent.profile().name(),
                turn.id(),
                iteration,
                agent.profile().maxIterations(),
                Instant.now(),
                null,
                null,
                request.callId(),
                call.id(),
                call.name());
        var result = tools.execute(audit, call.arguments(), agent.profile().tools());
        boolean present =
            turn.messages().stream()
                .anyMatch(
                    m ->
                        request.callId().equals(m.llmCallId()) && call.id().equals(m.toolCallId()));
        if (!present)
          memory.append(
              turn,
              new Message(
                  UUID.randomUUID(),
                  turn.id(),
                  turn.messages().size(),
                  Message.Role.TOOL,
                  result.content(),
                  request.callId(),
                  List.of(),
                  call.id(),
                  call.name()));
      }
    }
    throw new RuntimeFailure(RuntimeFailure.Code.ITERATION_LIMIT);
  }
}
