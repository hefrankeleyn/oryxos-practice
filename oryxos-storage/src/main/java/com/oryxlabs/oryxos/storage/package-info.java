/**
 * 持久化层：SQLite + Spring Data JPA。
 *
 * <p>职责：实现 core 中定义的存储契约（依赖倒置），落以下表：
 * {@code sessions}、{@code tool_invocations}、{@code llm_calls}、{@code scheduled_tasks}、
 * {@code task_executions}、{@code notify_channels}。
 *
 * <p>约束：审计表（tool_invocations / llm_calls）核心阶段就写入；表结构用手工维护的建表脚本，
 * 不依赖 {@code ddl-auto=update} 做演进。
 */
package com.oryxlabs.oryxos.storage;
