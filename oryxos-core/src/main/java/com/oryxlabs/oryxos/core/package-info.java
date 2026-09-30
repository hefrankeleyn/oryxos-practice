/**
 * OryxOS 核心模块（引擎层 + 核心抽象）。
 *
 * <p>职责：
 * <ul>
 * <li>核心抽象：{@code OryxTool} / {@code ToolResult}、{@code Profile}、{@code Session}、{@code Message}；</li>
 * <li>引擎：自实现的 {@code ReActLoop}（不使用 Spring AI Agent 抽象）、{@code PromptBuilder}、{@code ToolExecutor}；</li>
 * <li>统一入口：{@code AgentService.process(Session, String)}，CLI / Web / 定时三个入口共用；</li>
 * <li>Agent 定义：{@code AgentLoader}（扫描 {@code .oryxos/agents/}，frontmatter 派生 Profile）、{@code ProfileRegistry}、{@code ContextLoader}；</li>
 * <li>定时触发：{@code AgentScheduler}（第三触发源，钟推）。</li>
 * </ul>
 *
 * <p>约束：本模块只依赖抽象，不依赖任何具体模块；存储、Provider 等实现通过接口反向注入（依赖倒置）。
 */
package com.oryxlabs.oryxos.core;
