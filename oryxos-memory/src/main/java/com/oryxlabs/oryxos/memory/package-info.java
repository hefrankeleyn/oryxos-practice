/**
 * 核心能力三：Memory 记忆。
 *
 * <p>职责：
 * <ul>
 * <li>{@code MemoryService}：三层记忆统一门面，ReAct 循环只调用它；</li>
 * <li>会话记忆委托 {@code SessionManager}，长期记忆委托 {@code LongTermMemoryStore}；</li>
 * <li>核心阶段默认实现 {@code MarkdownMemoryStore}：{@code .oryxos/memory/MEMORY.md}，分「核心记忆 / 归档记忆」两区；</li>
 * <li>{@code MemoryTools}：内置工具 {@code save_memory}、{@code recall_memory}。</li>
 * </ul>
 *
 * <p>契约：长期记忆每次重新读取、不缓存；核心记忆区永不截断，截断与检索只作用于归档区。
 */
package com.oryxlabs.oryxos.memory;
