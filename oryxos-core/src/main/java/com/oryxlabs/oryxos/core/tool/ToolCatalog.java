package com.oryxlabs.oryxos.core.tool;

import java.util.List;
/** 工具目录仅提供元数据；不执行模型循环或审计。 */
public interface ToolCatalog { List<OryxTool> available(List<String> names); }
