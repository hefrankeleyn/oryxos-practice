package com.oryxlabs.oryxos.tool;

import com.oryxlabs.oryxos.core.tool.OryxTool;
/** 带真实 IO 动作声明的受信工具；纯计算工具无需伪造 IO 声明。 */
public interface SandboxedTool extends OryxTool { SandboxAction action(String arguments); }
