package com.oryxlabs.oryxos.tool;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import com.oryxlabs.oryxos.core.tool.*;
import java.util.*;

/** 统一工具元数据注册表；不调度模型、不写审计、不注册测试默认工具。 */
public final class ToolRegistry implements ToolCatalog {
  private final Map<String, OryxTool> tools;

  public ToolRegistry(List<OryxTool> entries) {
    Map<String, OryxTool> all = new LinkedHashMap<>();
    for (var t : entries)
      if (t.name() == null
          || !t.name().matches("[A-Za-z0-9_-]+")
          || all.putIfAbsent(t.name(), t) != null)
        throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, "agent.tools");
    tools = Map.copyOf(all);
  }

  public Set<String> names() {
    return tools.keySet();
  }

  public OryxTool get(String name) {
    return tools.get(name);
  }

  public List<OryxTool> available(List<String> allowed) {
    if (!tools.keySet().containsAll(allowed))
      throw new RuntimeFailure(RuntimeFailure.Code.TOOL_VALIDATION);
    return allowed.stream().map(tools::get).toList();
  }
}
