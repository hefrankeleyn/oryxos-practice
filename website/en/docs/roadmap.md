---
title: Roadmap
description: The three-phase OryxOS roadmap — from a single-node runtime kernel, to a distributed runtime, to cross-node Agent collaboration.
outline: deep
---

# Roadmap

> Slow is fast. Stay restrained and focused. First make "running and managing a fleet of Agents on one node" genuinely usable — then grow distributed capabilities on top of it.

| Phase | Shape | Focus | Status |
|---|---|---|---|
| **Phase 1** | Single-node runtime kernel | All five core capabilities; one node running and managing many Agents | 🚧 In progress |
| **Phase 2** | Distributed runtime | Stateless instances, external state, high availability, horizontal scaling | 📋 Planned |
| **Phase 3** | Cross-node Agent collaboration | An Agent communication layer, A2A, cross-node discovery and delegation | 💡 Vision |

## Phase 1: single-node runtime kernel (in progress)

Goal: one OryxOS instance on one server or container runs a set of Agents for one department or one use case.

- [x] Maven 9-module skeleton that compiles, packages and starts
- [x] CLI entry point with version information
- [ ] **LLM access**: provider abstraction, with DeepSeek and Kimi working
- [ ] **ReAct loop**: multi-step tool calls, message accumulation, iteration limits
- [ ] **Memory**: persisted sessions + `MEMORY.md` long-term memory + `save_memory` / `recall_memory`
- [ ] **Tools**: 9 built-in tools, MCP client (stdio), `@Tool` extensions, allow-list sandbox, notifications
- [ ] **Web Service**: 10 core REST endpoints
- [ ] Scheduler (`AgentScheduler`), SQLite persistence and audit, 12 CLI commands
- [ ] Three acceptance demos: daily weather, daily tech digest, daily GitHub trending
- [x] Project website

## Phase 2: distributed runtime (planned)

When a company goes from a single-department pilot to company-wide use, a single node hits three walls: load, failures and governance at scale.

The core principle is **stateless instances with external state**:

- Sessions and short-term context move to an in-memory store such as Redis
- Long-term memory and the Skill library move to PostgreSQL (pgvector for vector search)
- Audit logs and large files move to object storage
- Agent configuration and tenant data move to a config center and database

On top of that come the multi-instance concerns: each channel message consumed exactly once, each scheduled task run on exactly one instance (distributed locks or leases), and tenant isolation in storage and along every request path. Service discovery, rate limiting and tracing reuse the mature Java ecosystem — Nacos, Sentinel, Spring Cloud Gateway, SkyWalking.

## Phase 3: cross-node Agent collaboration (vision)

A company may run dozens or hundreds of Agents across departments and machines — even across partner organizations. A large purchase might need the ops Agent to confirm resources, the finance Agent to check the budget and the legal Agent to review the contract.

OryxOS is the Agent runtime on a single node. Connecting many OryxOS nodes — so Agents can discover each other, delegate reliably and share the context they need — is the job of a dedicated **Agent communication layer** built on the open A2A protocol. The two evolve separately and together form a complete distributed Agent OS.

## Cross-cutting capabilities (delivered alongside each phase)

| Area | Scope |
|---|---|
| Channels | WeCom, Feishu, DingTalk, Slack, email |
| Models | Provider failover, circuit breaking, task-based routing |
| Memory | Automatic extraction, semantic search, episodic memory |
| Tools & security | Tool policies (per-Agent allow/deny), container and microVM sandboxes, OryxOS as an MCP server |
| Governance | SSO (SAML / OIDC), multi-tenant RBAC, full audit with SIEM export |
| Operations | Prometheus metrics, web console, GraalVM native image |

## Long-term goal

Join the Apache Software Foundation and become an Apache top-level project.
