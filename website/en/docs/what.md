---
title: What is OryxOS
description: OryxOS is a Java-native Agent Harness OS for the enterprise — one directory defines an Agent, one runtime hosts them all.
outline: deep
---

# What is OryxOS

**OryxOS** is an open-source **Agent Harness OS**: an Agent runtime you install on your own Kubernetes cluster, VMs or bare metal. It gives every business Agent (ops, support, knowledge, sales assistants and more) shared model access, a reasoning loop, memory, tool calling, sandboxing and audit.

> One directory defines an Agent; one runtime hosts them all. Self-hosted — your data never leaves.

Teams do two things: **write an Agent directory** (a single `AGENT.md`) and **wire up a few tools**. Where messages come from, how the LLM is called, how context carries over and how audits are recorded — that is OryxOS's job.

![OryxOS architecture](/images/architecture.svg)

## Why OryxOS

Every company has work that belongs to Agents, yet most Agents never leave the demo. They get stuck on four barriers:

| Barrier | Today | The OryxOS way |
|---|---|---|
| Defining an Agent takes code | The people who know the business can't do it | One `AGENT.md`, written in natural language |
| Cloud platforms take your data | Regulated industries can't pass compliance | Self-hosted; data stays on your infrastructure |
| Execution is a black box | No audit, no allow-lists — nobody ships it | Mandatory sandbox; every LLM and tool call is audited in the database |
| Running a fleet is hard | Nobody provides an OS layer for many Agents | One runtime manages the lifecycle and shared services of many Agents |

The deeper point: **what keeps Agents from working reliably in production is rarely the model — it is the environment they run in.** OryxOS is not yet another Agent; it is the runtime that lets a fleet of Agents run reliably.

## Agent runtime vs. Agent Harness OS

- **Agent runtime**: the execution kernel that runs a single Agent — calling the model, executing tools, managing context, controlling the reasoning loop.
- **Agent Harness OS**: sits above the runtime and manages a fleet — Agent lifecycles, shared inbound channels and outbound integrations, shared memory, multi-tenancy and governance.

In operating-system terms, the runtime is a process's execution environment; the Agent Harness OS is the layer that manages many processes, schedules resources and provides shared services. **A runtime runs one Agent; OryxOS runs and manages all of them.**

The north-star formula:

```text
Natural language (AGENT.md) + Memory + Tools + MCP + Skills + Knowledge + Notify = an Agent
```

## Five core capabilities

| Capability | Description |
|---|---|
| **LLM access** | A provider abstraction over DeepSeek, Qwen, Kimi and other mainstream models; Agents never see the vendor. Multiple providers are distinguished by explicit mapping; local inference is supported |
| **ReAct loop** | A home-grown reasoning engine: the LLM decides whether to call a tool → OryxOS executes it and feeds back the result → reasoning continues until an answer or the iteration limit |
| **Memory** | Session memory plus long-term memory; long-term memory lives in a human-readable `MEMORY.md` with keyword search, with room to upgrade to vector search |
| **Tools** | Nine built-in tools (files, shell, HTTP, memory, notify); three extension tiers: zero-code Agent directory + MCP, light-code custom MCP server, full-code `@Tool` Bean |
| **Web Service** | Every capability is exposed over a REST API, so any language can integrate over HTTP |

## Key features

- 🤖 **One directory = one Agent**: any directory with an `AGENT.md` is an Agent — no code, many Agents per instance
- ☕ **Java-native**: JDK 21 + Spring Boot 3, a single executable JAR that fits your existing Java toolchain
- 🔒 **Under your control**: runs on your infrastructure, data never leaves, no cloud lock-in
- 🛡️ **Secure by design**: file, command and network allow-lists on every tool call; secrets via environment variables; end-to-end audit
- 🧠 **Home-grown ReAct**: the core loop is implemented in-house, not borrowed from an Agent framework
- 🔌 **Open standards**: MCP for tools, A2A for collaboration, Agent directories modeled on Anthropic Agent Skills
- ⏰ **Scheduled runs**: Agents can run on a cron schedule and push results to your team IM
- 🌐 **Stateless and scalable**: stateless instances with externalized state, ready for a distributed future

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
