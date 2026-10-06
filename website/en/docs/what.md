---
title: What is OryxOS
description: OryxOS is a Java-native Agent Harness OS for the enterprise — one directory defines an Agent, one runtime hosts them all.
outline: deep
---

# What is OryxOS

::: warning Engineering skeleton, not a usable Agent platform
Nine Maven modules, a Boot entry, a CLI version entry and a bilingual website exist. Provider, ReAct, Memory, Tool, REST and business persistence are not implemented yet. This page describes the product direction and target design. Check the [implementation status](https://github.com/hefrankeleyn/oryxos-practice/blob/main/docs/IMPLEMENTATION_STATUS.md) for actual progress.
:::

**OryxOS** is an open-source **Agent Harness OS**: an Agent runtime you install on your own Kubernetes cluster, VMs or bare metal. It gives every business Agent (ops, support, knowledge, sales assistants and more) shared model access, a reasoning loop, memory, tool calling, sandboxing and audit.

> One directory defines an Agent; one runtime provides a shared execution environment. Start with a single-node Runtime MVP, then grow toward an Agent OS.

Self-hosting keeps the workspace and storage on your infrastructure. Cloud-model requests still go to the configured Provider; data egress depends on the actual deployment and access settings.

Teams do two things: **write an Agent directory** (a single `AGENT.md`) and **wire up a few tools**. Where messages come from, how the LLM is called, how context carries over and how audits are recorded — that is OryxOS's job.

![OryxOS target architecture: unified entry, ReAct, memory, tools and audit](/images/architecture.svg)

[Open the full diagram](/images/architecture.svg)

## Why OryxOS

Every company has work that belongs to Agents, yet most Agents never leave the demo. They get stuck on four barriers:

| Barrier | Today | The OryxOS way |
|---|---|---|
| Defining an Agent takes code | The people who know the business can't do it | One `AGENT.md`, written in natural language |
| Data boundaries are hard to control | Storage and model access have unclear boundaries | Self-host storage and explicitly choose cloud or local Providers |
| Execution is a black box | No audit, no allow-lists — nobody ships it | Mandatory sandbox; every LLM and tool call is audited in the database |
| Running a fleet is hard | Nobody provides an OS layer for many Agents | One runtime manages the lifecycle and shared services of many Agents |

The deeper point: **what keeps Agents from working reliably in production is rarely the model — it is the environment they run in.** OryxOS is not yet another Agent; it is the runtime that lets a fleet of Agents run reliably.

## Agent runtime vs. Agent Harness OS

- **Agent runtime**: the execution kernel that runs a single Agent — calling the model, executing tools, managing context, controlling the reasoning loop.
- **Agent Harness OS**: sits above the runtime and manages a fleet — Agent lifecycles, shared inbound channels and outbound integrations, shared memory, multi-tenancy and governance.

The Agent OS is the long-term direction. The current core stage covers only a single-node runtime, not multi-tenancy, team orchestration or cross-node collaboration.

In operating-system terms, the runtime is a process's execution environment; the Agent Harness OS is the layer that manages many processes, schedules resources and provides shared services. **A runtime runs one Agent; OryxOS runs and manages all of them.**

The north-star formula:

```text
Natural language (AGENT.md) + Memory + Tools + MCP + Skills + Knowledge + Notify = an Agent
```

## Five core capabilities (development targets)

| Capability | Description |
|---|---|
| **LLM access** | A provider abstraction over DeepSeek, Qwen, Kimi and other mainstream models; Agents never see the vendor. Multiple providers are distinguished by explicit mapping; local inference is supported |
| **ReAct loop** | A home-grown reasoning engine: the LLM decides whether to call a tool → OryxOS executes it and feeds back the result → reasoning continues until an answer or the iteration limit |
| **Memory** | Session memory plus long-term memory; long-term memory lives in a human-readable `MEMORY.md` with keyword search, with room to upgrade to vector search |
| **Tools** | Nine built-in tools (files, shell, HTTP, memory, notify); three extension tiers: zero-code Agent directory + MCP, light-code custom MCP server, full-code `@Tool` Bean |
| **Web Service** | Every capability is exposed over a REST API, so any language can integrate over HTTP |

## Target features

- 🤖 **One directory = one Agent**: any directory with an `AGENT.md` is an Agent — no code, many Agents per instance
- ☕ **Java-native**: JDK 21 + Spring Boot 3, fitting your existing Java toolchain. Boot and CLI currently ship separately; the final packaging contract remains open
- 🔒 **Under your control**: runs on your infrastructure, with explicit storage and model-access boundaries
- 🛡️ **Execution boundaries**: file, command and network allow-lists, environment-injected secrets and persisted audit. An allow-list is not strong isolation
- 🧠 **Home-grown ReAct**: the core loop is implemented in-house, not borrowed from an Agent framework
- 🔌 **Open standards**: MCP for tools, on-demand Skills; A2A collaboration belongs to a later extension stage
- ⏰ **Scheduled runs**: Agents can run on a cron schedule and push results to your team IM
- 🌐 **Room to grow**: single-node SQLite and files first; externalized state and distributed operation are later roadmap items

## Design principles

- **The runtime over any single Agent**: the key deliverable is an environment where any Agent runs reliably
- **Own the core**: the reasoning loop is home-grown; model protocol adapters reuse mature libraries
- **Configuration is the Agent**: an Agent is defined by configuration, not code
- **Open standards**: interoperate with the ecosystem instead of inventing protocols
- **Stateless instances, external state**: the prerequisite for scaling out
- **Security is the foundation, not a patch**: controlled sources, least privilege, mandatory sandbox, no plaintext secrets, full audit
- **Deliberate phasing**: get the single-node kernel right first; every architectural upgrade must be justified by real usage

## How OryxOS relates to other projects

| Category | Examples | Relationship |
|---|---|---|
| Agent OS | OpenClaw, Hermes Agent | **Same category, different focus**: they target individuals and small teams; OryxOS targets regulated enterprises |
| Orchestration platforms | Dify, Coze | **Complementary**: they can sit on top and call OryxOS's API |
| Agent frameworks | Spring AI, LangChain4j | **Reused**: OryxOS's LLM layer is built on Spring AI Alibaba |

In short: a framework hands you materials to build a house; an orchestration platform scripts a workflow; OryxOS hands you a move-in-ready, governable, auditable home for your Agents.

## Next steps

- [Quick Start](./quick-start): build from source and see how 1.0 will be used
- [Architecture](./architecture): layers, key decisions and modules
- [Roadmap](./roadmap): from a single-node kernel to distributed Agent collaboration
