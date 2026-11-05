package com.oryxlabs.oryxos.core.tool;

import java.util.List;

/** 统一工具执行适配端口，由 ToolExecutor 调度。 */
public interface ToolInvoker {
  ToolResult invoke(String name, String arguments, List<String> allowed);
}
