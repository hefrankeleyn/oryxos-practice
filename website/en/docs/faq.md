---
title: FAQ
description: Frequently asked questions about OryxOS — positioning, technology choices, security and production readiness.
outline: deep
---

# FAQ

## Positioning

### How is OryxOS different from OpenClaw and Hermes Agent?

All three are Agent OSes — same category, different focus. OpenClaw (Node.js) targets individuals and developers; Hermes Agent (Python) targets individuals and small teams. OryxOS is written in Java and targets **regulated enterprises** directly: self-hosted, fully auditable, and able to fit into existing Java stacks and IT governance. All three use markdown + frontmatter directories, so vetted community Skills can in principle be imported into OryxOS.

### Does OryxOS compete with Dify or Coze?

No — they are complementary. Dify and Coze are workflow orchestration platforms whose output is an explicit flow chart. OryxOS's output is a long-running Agent that decides its next step at runtime from its instructions, tools and context. They combine well: Dify as the application layer, calling OryxOS's API.

### How is it different from Spring AI or LangChain4j?

Frameworks give you libraries to write an Agent in code, and you run it yourself. OryxOS is a ready-to-run service; teams don't write Agent backend code. The relationship is reuse: OryxOS's LLM layer is built on Spring AI Alibaba.

### Will OryxOS do task decomposition or multi-Agent orchestration?

Not in the core phase. OryxOS stays at the runtime layer and does not build visual orchestration or complex task decomposition. Cross-node Agent collaboration is the phase-three vision in the [roadmap](./roadmap).

## Usage

### Do I need to write code to define an Agent?

No. Put an `AGENT.md` in `.oryxos/agents/<name>/`: the frontmatter says which model, which tools and whether to run on a schedule; the body describes the task in natural language. To reach external systems, reuse existing community MCP servers first.

### What is the difference between an Agent and a Skill?

**One directory defines one Agent** (`.oryxos/agents/<name>/AGENT.md`) — it decides how it runs and what it does. **A Skill is a shared capability template** (`.oryxos/skills/<name>/SKILL.md`); an Agent picks the Skills it uses through symlinks in its own `skills/` directory. Only a Skill's name and description enter the prompt each turn; the model reads the body on demand, keeping the context lean.

### Which LLMs are supported?

Mainstream models via Spring AI Alibaba: DeepSeek, Qwen, Kimi, Zhipu, Hunyuan, Doubao, Anthropic, OpenAI and more — plus self-hosted inference such as Ollama or vLLM. The OpenAI-compatible protocol is the de-facto standard; any provider implementing it plugs straight in.

### How do business systems integrate?

Through the REST API. Use `POST /api/v1/agents/{name}/invoke` for synchronous one-shot calls; create a session and send messages for conversations; alerting systems and CI/CD can trigger Agents over HTTP. Any language that speaks HTTP works.

## Security and data

### Does any data leave my infrastructure?

OryxOS itself collects and sends nothing; sessions, memory and audit data stay on your infrastructure. The only outbound traffic goes to the LLM APIs, MCP servers and notification channels you configure — and with self-hosted inference, data never has to leave at all.

### How are tool calls kept safe?

Every file, command and network access passes the sandbox: path allow-lists (with traversal protection), command allow-lists (no shell interpretation) and HTTP domain allow-lists. Every call — including rejected ones — is written to the audit tables. API keys are injected via environment variables, never stored in plain text.

### Is it production-ready?

Not yet. OryxOS is in phase one. The core-phase sandbox is an application-level allow-list that stops model mistakes but cannot contain deliberately malicious code. Authentication, multi-tenancy and container-level isolation come in later phases.

## Technology choices

### Why Java?

The Java and Spring ecosystem has mature building blocks at every enterprise backend layer — except the Agent OS layer. Enterprises run ERP, CRM, SSO and monitoring on Java. As a standard Spring Boot application, OryxOS calls existing Java services directly, reuses your operations toolchain and goes through your existing code review and compliance processes.

### Why implement the ReAct loop yourselves?

The loop is the heart of an Agent; owning it means full control over iterations, message accumulation, error handling and audit writes. Spring AI is used only for provider protocol conversion and tool schemas; its automatic tool execution is disabled so tools are never called twice.

### Why SQLite instead of a vector database in the core phase?

To keep a single binary that just runs. Sessions and audit go to SQLite; long-term memory goes to `MEMORY.md` with keyword search — the shortest path to a working system. The memory interface already leaves room for semantic search, which arrives in the extension phase.

## Project

### What is the license?

Apache License 2.0.

### How can I contribute?

OryxOS is an oryx-labs project — a hobby-driven AI community that builds with AI coding. Issues, design discussions and pull requests are welcome on [GitHub](https://github.com/hefrankeleyn/oryxos-practice).
