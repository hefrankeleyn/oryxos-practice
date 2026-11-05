package com.oryxlabs.oryxos.core.model;

import java.nio.file.Path;

/** 一个受控目录定义一个 Agent；配置派生后不维护独立 Profile 文件。 */
public record AgentDefinition(Path workspace, Path directory, ProfileSnapshot profile) {}
