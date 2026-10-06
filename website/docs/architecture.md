---
title: 系统架构
description: OryxOS 的分层架构、ReAct 循环、记忆与工具体系、沙箱与审计，以及关键技术决策和模块划分。
outline: deep
---

# 系统架构

OryxOS 的目标是一个 **Spring Boot 3 单机运行时**，基于 **JDK 21**，通过 **Spring AI / Spring AI Alibaba** 适配模型协议，自实现 **ReAct 循环**。当前 Boot 与 CLI 是两个独立 JAR，最终打包契约仍待收口。

::: warning 目标架构，业务能力尚未实现
当前仓库是 Maven 工程骨架。下图及本页接口、流程、审计与存储均为目标设计，不是运行证据；真实进度见 [实施状态](https://github.com/hefrankeleyn/oryxos-practice/blob/main/docs/IMPLEMENTATION_STATUS.md)。
:::

> 技术栈一句话：JDK 21 + Spring Boot 3 + Spring AI Alibaba + 自实现 ReAct + SQLite + Picocli。

![OryxOS 目标逻辑架构](/images/architecture.svg)

[打开完整架构图](/images/architecture.svg)

图中展示逻辑调用关系，不是 Maven 依赖关系：`core` 定义接口，能力模块实现接口，`boot` 聚合模块。

## 分层视图

| 层 | 组成 | 职责 |
|---|---|---|
| **接入层** | CLI Channel、Web Service（REST）、`AgentScheduler` | 消息进出。CLI 和 REST 是"人推"，定时任务是"钟推" |
| **统一入口** | `AgentService` | 三个入口共用的编排者，不区分消息来源 |
| **引擎层** | `ReActLoop`、`PromptBuilder`、`ToolExecutor` | Agent 的大脑 |
| **能力层** | Provider、Memory、Tool | 给引擎提供 LLM 调用、上下文和执行能力 |
| **基础层** | `AgentLoader`、`ContextLoader`、`ConfigLoader`、SQLite、文件系统 | Agent 定义加载、配置与密钥、持久化 |

一句话：**Provider、Memory、Tool 三块能力供养 ReAct 引擎，引擎的能力通过 CLI、Web Service、定时任务三个入口对外提供。**

核心执行链路收敛在一个 JVM 进程内；LLM API、外部 MCP server 与企业 IM 位于进程边界之外。工具动作由 Sandbox 按类型校验；LLM 与 Tool 调用都要落库审计。使用云端 LLM 时，请求会发送到 Provider，不应将工具白名单描述成所有模型网络访问的隔离边界。

## 一次消息的处理流程

```text
消息从 CLI / REST API / 定时任务进来
  → AgentService.process(Session, message)
    → PromptBuilder 组装 Prompt
       （AGENT.md 正文 + Bootstrap + Skill 元数据 + 当前时间 + 长期记忆 + 对话历史 + 工具列表）
    → ProviderService 调用 LLM                         ── 写 llm_calls
       ├─ 无工具调用 → 返回最终响应
       └─ 有工具调用 → ToolExecutor：查找工具 → 沙箱校验 → 执行  ── 写 tool_invocations
                      → 结果追加到对话历史 → 回到组装 Prompt
    → 达到最大迭代次数（默认 10）强制结束
  → Session 持久化
```

## 核心能力

### 对接 LLM

`ProviderService` 统一管理所有 Provider，对 ReAct 循环屏蔽厂商差异。多个 Provider 并存时，维护一份**显式的 provider name → `ChatModel` 映射**，不靠扫描容器里的 Bean 类型来区分。每次调用记录 token 用量、Provider 和模型，写入 `llm_calls`。

### ReAct 循环

`ReActLoop` 是 OryxOS 最核心的一段代码，自己实现，不依赖 Spring AI 的 Agent 抽象。Spring AI 在 OryxOS 里只做两件事：**Provider 协议转换**和 **`@Tool` 的 JSON Schema 生成**；它自带的自动工具执行被禁用，工具的调度完全由 `ToolExecutor` 负责，避免工具被调用两次。

### 记忆

`MemoryService` 是三层记忆的统一门面，ReAct 循环只问它一个接口：

- **会话记忆**：委托 `SessionManager`，持久化到 SQLite，重启可恢复；过长时截断早期对话
- **长期记忆**：委托 `LongTermMemoryStore`，默认实现是 `.oryxos/memory/MEMORY.md`，分「核心记忆 / 归档记忆」两区；核心区永不截断，截断和检索只作用于归档区；每次重新读取、不缓存，写入后下一轮立即可见
- **情景记忆**：扩展阶段补齐

Agent 通过 `save_memory` / `recall_memory` 两个内置工具主动写入和检索。

### 工具体系

所有工具（内置、MCP、`@Tool` Bean）都被包装成统一的 `OryxTool`，ReAct 循环不感知工具来源：

| 方式 | 门槛 | 做法 |
|---|---|---|
| 零代码（主推） | 最低 | 写 Agent 目录 + 在 `mcp_servers.yaml` 复用现成 MCP server |
| 轻代码 | 中 | 用任意语言写 MCP server，OryxOS 作为 MCP Client 连接 |
| 重代码 | 高 | 用 `@Tool` 注解写 Java Spring Bean，进程内调用 |

**`AGENT.md` 和 Skill 不是工具。** Agent 正文由 `ContextLoader` 注入 system prompt；Agent 绑定的 Skill 每轮只注入名称、描述和路径，正文由模型用 `read_file` 按需读取（渐进式披露）。

### 对外服务

Web Service 是 OryxOS 的对外门面，核心阶段提供 10 个 REST 端点（会话管理 4 个、Agent 调用 1 个、Agent / 记忆 / 工具查询 3 个、健康与信息 2 个）。约束：单条消息 ≤ 32KB，历史最多返回 100 条，Agent 调用 60 秒超时。

## 安全：沙箱与审计

### 沙箱

沙箱遵循"**接口先行**"：先定一个不携带任何实现细节的接口 `Sandbox.enforce(action)`，核心阶段只实现应用层白名单这一档：

| 动作 | 校验 |
|---|---|
| 文件读写 | 路径标准化后比对白名单，防止 `../` 路径穿越 |
| Shell 命令 | 只执行白名单内的可执行文件，参数数组直传，不经 shell 解释 |
| HTTP 请求 | 解析 host 后按域名白名单匹配 |
| SMTP | 按 `host:port` 精确放行 |

扩展阶段按需求升级到容器隔离、microVM，**接口不变，只新增实现**。

::: warning
应用层白名单是"劝阻级"防线，防的是模型误操作，防不住蓄意绕过。核心阶段不要用它运行完全不可信的代码，或对外提供多租户服务。
:::

### 审计

每次 LLM 调用写 `llm_calls`，每次工具调用（包括失败和沙箱拒绝）写 `tool_invocations`。审计数据**从第一天就落库**，而不是事后从日志里反解析。

## 关键技术决策

| # | 决策 | 选择 | 理由 |
|---|---|---|---|
| 1 | ReAct 实现 | 自实现 | 完全可控，保留定制循环行为的空间 |
| 2 | Spring AI 边界 | 只用协议转换 + schema 生成，禁用自动工具执行 | 避免工具被调两次，循环由 OryxOS 掌控 |
| 3 | 执行模型 | 同步阻塞 + Java 21 virtual thread | 代码直观，单节点撑高并发 |
| 4 | 工具注册 | `@Tool` 注解 + `OryxTool` 抽象 | 统一内置工具与 MCP 工具 |
| 5 | HTTP 服务层 | Spring MVC + virtual thread | 同步直观，后续用 `SseEmitter` 支持流式 |
| 6 | 沙箱 | 接口先行 + 应用层白名单 | `SecurityManager` 在 JDK 21 已不可用；换实现不改调用方 |
| 7 | 持久化 | SQLite + Spring Data JPA + `MEMORY.md` | 单二进制部署，审计表第一天写入 |

## 数据持久化

| 数据 | 存储 | 说明 |
|---|---|---|
| 会话 | SQLite `sessions` | 对话历史 JSON 序列化，跨重启恢复 |
| 审计 | SQLite `tool_invocations`、`llm_calls` | 每次调用一条记录 |
| 定时任务 | SQLite `scheduled_tasks`、`task_executions` | 任务状态与执行历史 |
| 通知渠道 | SQLite `notify_channels` | 全局注册表，Agent 按名称引用 |
| Agent 定义、Bootstrap、长期记忆、MCP 配置 | 文件系统 `.oryxos/` | 用户可直接编辑、git 跟踪、备份 |

## 模块划分

OryxOS 是一个 Maven 多模块项目，核心阶段共 9 个模块：

| 模块 | 职责 |
|---|---|
| `oryxos-core` | 核心抽象与引擎：`ReActLoop`、`PromptBuilder`、`ToolExecutor`、`AgentService`、`AgentLoader`、`ContextLoader`、`AgentScheduler` |
| `oryxos-provider` | LLM Provider 抽象，provider name 显式映射 |
| `oryxos-memory` | `MemoryService` 门面、长期记忆存储、记忆工具 |
| `oryxos-tool` | 内置工具、MCP Client、`ToolRegistry`、沙箱、通知推送 |
| `oryxos-channel-cli` | CLI 对话渠道 |
| `oryxos-web` | REST API、统一异常处理、OpenAPI 文档 |
| `oryxos-storage` | SQLite 持久化 |
| `oryxos-cli` | Picocli 命令行入口、配置与密钥加载 |
| `oryxos-boot` | Spring Boot 启动与依赖聚合 |

模块之间通过接口解耦：`oryxos-core` 不依赖任何内部模块，实现类依赖 core 中的接口。扩展阶段加新渠道或新工具时只加新模块，不改核心引擎。
