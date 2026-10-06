---
title: Architecture
description: OryxOS layers, the ReAct loop, memory and tools, sandbox and audit, key technical decisions and modules.
outline: deep
---

# Architecture

OryxOS targets a **single-node Spring Boot 3 runtime** on **JDK 21**, using **Spring AI / Spring AI Alibaba** for model protocol adaptation and an in-house **ReAct loop**. Boot and CLI currently have separate executable JARs; the final packaging contract remains open.

::: warning Target architecture, not implemented business capabilities
The repository currently contains the Maven engineering skeleton. The diagram, interfaces, flows, audit and storage on this page are target designs, not runtime evidence. See the [implementation status](https://github.com/hefrankeleyn/oryxos-practice/blob/main/docs/IMPLEMENTATION_STATUS.md) for actual progress.
:::

> The stack in one line: JDK 21 + Spring Boot 3 + Spring AI Alibaba + home-grown ReAct + SQLite + Picocli.

![OryxOS target logical architecture](/images/architecture.svg)

[Open the full diagram](/images/architecture.svg)

This is a logical call-flow diagram, not a Maven dependency graph: core defines contracts, capability modules implement them, and boot assembles the modules.

## Layers

| Layer | Components | Responsibility |
|---|---|---|
| **Entry** | CLI Channel, Web Service (REST), `AgentScheduler` | Messages in and out. CLI and REST are human-triggered; the scheduler is clock-triggered |
| **Unified entry point** | `AgentService` | The single orchestrator shared by all three entries, agnostic to the source |
| **Engine** | `ReActLoop`, `PromptBuilder`, `ToolExecutor` | The Agent's brain |
| **Capabilities** | Provider, Memory, Tool | LLM calls, context and execution for the engine |
| **Foundation** | `AgentLoader`, `ContextLoader`, `ConfigLoader`, SQLite, file system | Agent definitions, configuration and secrets, persistence |

In one sentence: **Provider, Memory and Tool feed the ReAct engine, and the engine is exposed through three entries — CLI, Web Service and the scheduler.**

The core execution path sits inside one JVM. LLM APIs, external MCP servers and team IM systems sit outside that process. Sandbox checks apply to tool actions by type; LLM and Tool calls both require persisted audit. Cloud-model requests go to the Provider—the tool allow-list is not an isolation boundary for all model traffic.

## How a message is processed

```text
A message arrives from the CLI / REST API / scheduler
  → AgentService.process(Session, message)
    → PromptBuilder assembles the prompt
       (AGENT.md body + bootstrap files + Skill metadata + current time + long-term memory + history + tools)
    → ProviderService calls the LLM                                  ── writes llm_calls
       ├─ no tool call → return the final answer
       └─ tool call → ToolExecutor: look up → sandbox check → run    ── writes tool_invocations
                      → append the result to history → assemble the prompt again
    → stop at the iteration limit (default 10)
  → persist the session
```

## Core capabilities

### LLM access

`ProviderService` manages all providers and hides vendor differences from the ReAct loop. With several providers side by side, it keeps an **explicit provider name → `ChatModel` map** instead of guessing from bean types. Every call records token usage, provider and model in `llm_calls`.

### ReAct loop

`ReActLoop` is the most important code in OryxOS. It is implemented in-house rather than on Spring AI's Agent abstractions. Spring AI does exactly two things here: **provider protocol conversion** and **JSON Schema generation for `@Tool`**. Its automatic tool execution is disabled; `ToolExecutor` alone schedules tools, so no tool ever runs twice.

### Memory

`MemoryService` is the single facade over all memory layers; the ReAct loop only talks to it:

- **Session memory**: delegated to `SessionManager`, persisted in SQLite and restored after restarts; early turns are truncated when too long
- **Long-term memory**: delegated to `LongTermMemoryStore`, by default `.oryxos/memory/MEMORY.md` with a *core* and an *archival* section. The core section is never truncated; truncation and search apply to the archive only. It is re-read on every turn with no cache, so new memories are visible immediately
- **Episodic memory**: coming in the extension phase

Agents write and search memory with the built-in `save_memory` and `recall_memory` tools.

### Tools

Every tool — built-in, MCP or `@Tool` Bean — is wrapped as a uniform `OryxTool`, so the loop never cares where a tool comes from:

| Tier | Effort | How |
|---|---|---|
| Zero code (recommended) | Lowest | Write an Agent directory and reuse MCP servers via `mcp_servers.yaml` |
| Light code | Medium | Write an MCP server in any language; OryxOS connects as an MCP client |
| Full code | Highest | Write a Java Spring Bean with `@Tool`, called in-process |

**`AGENT.md` and Skills are not tools.** The Agent body is injected into the system prompt by `ContextLoader`. Bound Skills contribute only their name, description and path each turn; the model reads a Skill's body with `read_file` when it needs it (progressive disclosure).

### Web Service

The Web Service is OryxOS's front door. The core phase ships 10 REST endpoints: 4 for sessions, 1 for Agent invocation, 3 for Agent / memory / tool queries and 2 for health and info. Limits: 32 KB per message, at most 100 history entries per response, a 60-second timeout per invocation.

## Security: sandbox and audit

### Sandbox

The sandbox is **interface-first**: a neutral `Sandbox.enforce(action)` interface with one implementation in the core phase — application-level allow-lists:

| Action | Check |
|---|---|
| File read/write | Normalized path matched against the allow-list; blocks `../` traversal |
| Shell command | Only allow-listed executables; argument arrays passed directly, never through a shell |
| HTTP request | Host matched against the domain allow-list |
| SMTP | Exact `host:port` allow-list |

Later phases add container and microVM isolation — **the interface stays the same; only new implementations are added.**

::: warning
Application-level allow-lists stop a model's mistakes, not a determined attacker. Do not run fully untrusted code or offer multi-tenant service on the core-phase sandbox.
:::

### Audit

Every LLM call is written to `llm_calls`; every tool call — including failures and sandbox rejections — to `tool_invocations`. Audit data is **stored in the database from day one**, not reconstructed from logs later.

## Key technical decisions

| # | Decision | Choice | Why |
|---|---|---|---|
| 1 | ReAct implementation | Home-grown | Full control; room to customize the loop |
| 2 | Spring AI boundary | Protocol conversion + schema generation only; auto tool execution disabled | No double tool calls; OryxOS owns the loop |
| 3 | Execution model | Synchronous + Java 21 virtual threads | Plain code, high concurrency on one node |
| 4 | Tool registration | `@Tool` + `OryxTool` abstraction | One interface for built-in and MCP tools |
| 5 | HTTP layer | Spring MVC + virtual threads | Straightforward; streaming later via `SseEmitter` |
| 6 | Sandbox | Interface-first + application allow-lists | `SecurityManager` is gone in JDK 21; swapping implementations never touches callers |
| 7 | Persistence | SQLite + Spring Data JPA + `MEMORY.md` | Single-binary deployment; audit tables written from day one |

## Persistence

| Data | Store | Notes |
|---|---|---|
| Sessions | SQLite `sessions` | History serialized as JSON; survives restarts |
| Audit | SQLite `tool_invocations`, `llm_calls` | One row per call |
| Schedules | SQLite `scheduled_tasks`, `task_executions` | Task state and run history |
| Notification channels | SQLite `notify_channels` | Global registry referenced by name |
| Agent definitions, bootstrap files, long-term memory, MCP config | File system `.oryxos/` | Editable, git-trackable, easy to back up |

## Modules

OryxOS is a Maven multi-module project with 9 modules in the core phase:

| Module | Responsibility |
|---|---|
| `oryxos-core` | Core abstractions and engine: `ReActLoop`, `PromptBuilder`, `ToolExecutor`, `AgentService`, `AgentLoader`, `ContextLoader`, `AgentScheduler` |
| `oryxos-provider` | LLM provider abstraction with explicit name mapping |
| `oryxos-memory` | `MemoryService` facade, long-term memory store, memory tools |
| `oryxos-tool` | Built-in tools, MCP client, `ToolRegistry`, sandbox, notifications |
| `oryxos-channel-cli` | CLI chat channel |
| `oryxos-web` | REST API, global error handling, OpenAPI docs |
| `oryxos-storage` | SQLite persistence |
| `oryxos-cli` | Picocli entry point, configuration and secret loading |
| `oryxos-boot` | Spring Boot bootstrap and aggregation |

Modules are decoupled through interfaces: `oryxos-core` depends on no other module, and implementations depend on interfaces declared in core. New channels or tools are added as new modules without touching the engine.
