/**
 * 核心能力四：Tool 体系（内置 Tool、MCP Client、ToolRegistry、Sandbox、Notify 合一模块）。
 *
 * <p>职责：
 * <ul>
 * <li>内置 Tool：{@code FileTools}（read_file / write_file / list_dir）、{@code ShellTools}（shell）、
 * {@code HttpTools}（http_get / http_post）、{@code NotifyTools}（notify）；</li>
 * <li>{@code ToolRegistry}：统一注册内置 Tool、{@code @Tool} Bean 与 MCP Tool，全部包装为 {@code OryxTool}；</li>
 * <li>{@code McpClientService} / {@code McpToolAdapter}：连接外部 MCP server（先实现 stdio）；</li>
 * <li>{@code Sandbox} 接口 + {@code WhitelistSandbox}：文件路径 / 命令 / HTTP 域名 / SMTP 端点白名单，不使用 SecurityManager；</li>
 * <li>{@code NotifyChannelAdapter} 接口 + {@code WebhookNotifyAdapter}。</li>
 * </ul>
 *
 * <p>约束：Agent 目录（AGENT.md）与 Skill 不是 Tool，不在本模块注册。
 */
package com.oryxlabs.oryxos.tool;
