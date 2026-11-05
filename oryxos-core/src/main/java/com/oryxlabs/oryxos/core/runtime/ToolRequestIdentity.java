package com.oryxlabs.oryxos.core.runtime;

import java.util.UUID;

/** 去重只限本次处理、模型响应和原请求 ID；新模型轮不按参数哈希误去重。 */
public record ToolRequestIdentity(UUID runId, UUID llmCallId, String toolCallId) {}
