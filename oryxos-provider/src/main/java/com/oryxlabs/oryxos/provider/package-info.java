/**
 * 核心能力一：对接 LLM（Provider 抽象）。
 *
 * <p>职责：
 * <ul>
 * <li>{@code ProviderService}：统一管理所有 LLM Provider，对 ReAct 循环屏蔽厂商差异；</li>
 * <li>维护 <b>provider name → ChatModel 的显式映射</b>，禁止靠扫描容器中所有 ChatModel Bean 区分 Provider；</li>
 * <li>Function Calling 适配：把 {@code OryxTool} 转成 Spring AI 工具定义，<b>只做格式转换、禁用自动 tool 执行</b>；</li>
 * <li>每次 LLM 调用记录 token 用量，写入 {@code llm_calls} 审计表。</li>
 * </ul>
 */
package com.oryxlabs.oryxos.provider;
