package com.oryxlabs.oryxos.core.runtime;

import com.oryxlabs.oryxos.core.agent.ContextLoader;
import com.oryxlabs.oryxos.core.memory.MemoryService;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.provider.ProviderRequest;
import com.oryxlabs.oryxos.core.tool.ToolCatalog;
import java.util.UUID;

/** 单次构建只读取一次历史投影，系统指令和当前链不参与历史轮数裁剪。 */
public final class PromptBuilder {
  private final MemoryService memory;
  private final ContextLoader context;
  private final ToolCatalog tools;

  public PromptBuilder(MemoryService memory, ContextLoader context, ToolCatalog tools) {
    this.memory = memory;
    this.context = context;
    this.tools = tools;
  }

  public ProviderRequest build(Session session, Turn turn, AgentDefinition agent, int iteration) {
    return new ProviderRequest(
        UUID.randomUUID(),
        session.id(),
        turn.id(),
        iteration,
        agent.profile(),
        context.read(agent),
        memory.project(session, turn, agent.profile().maxHistoryTurns()),
        tools.available(agent.profile().tools()));
  }
}
