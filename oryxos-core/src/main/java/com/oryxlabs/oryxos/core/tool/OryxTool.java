package com.oryxlabs.oryxos.core.tool;

/** 工具元数据与执行契约；具体实现负责在真实 IO 前校验 Sandbox。 */
public interface OryxTool {
    String name(); String description(); String inputSchema();
    String execute(String arguments) throws Exception;
}
