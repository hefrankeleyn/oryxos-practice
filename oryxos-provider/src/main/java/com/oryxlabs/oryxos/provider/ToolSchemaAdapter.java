package com.oryxlabs.oryxos.provider;

import com.oryxlabs.oryxos.core.tool.OryxTool;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.*;
/** 只向模型暴露工具元数据；即使误触发框架 callback，也不能执行真实工具。 */
public final class ToolSchemaAdapter {
 public ToolCallback adapt(OryxTool tool){return new ToolCallback(){
  public ToolDefinition getToolDefinition(){return new DefaultToolDefinition(tool.name(),tool.description(),tool.inputSchema());}
  public String call(String ignored){throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);}
 };}
}
