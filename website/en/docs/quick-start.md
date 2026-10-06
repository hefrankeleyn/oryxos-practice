---
title: Quick Start
description: Build OryxOS from source and see how version 1.0 defines Agents, chats with them and runs as a service.
outline: deep
---

# Quick Start

::: warning In development
OryxOS is in **phase one: the single-node runtime kernel**. Part one of this page **works today**; part two is the **target 1.0 usage**, and commands and interfaces may change before release.
:::

## Requirements

- **JDK 21+**
- **Maven 3.9+**
- Linux or macOS

Building the skeleton and running the CLI version entry require no LLM API key. Model configuration belongs to the unimplemented target runtime.

## Part 1: Build from source (works today)

```bash
git clone https://github.com/hefrankeleyn/oryxos-practice.git
cd oryxos-practice
mvn clean package
```

The build produces two executable JARs:

| Artifact | Purpose |
|---|---|
| `oryxos-boot/target/oryxos.jar` | Spring Boot skeleton; no business REST API yet |
| `oryxos-cli/target/oryxos-cli-<version>-exec.jar` | CLI entry; version, help and no-argument output only |

Check that the CLI works:

```bash
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --help
```

```text
OryxOS 0.1.0-SNAPSHOT
构建时间: 2026-09-30T10:25:35Z
Java:     21.0.8 (Homebrew)
系统:     Mac OS X 26.6.2 (aarch64)
```

You can also start the Boot skeleton:

```bash
java -jar oryxos-boot/target/oryxos.jar
```

It provides no Agent invocation, business REST or health endpoints yet. A listening port does not prove a working Runtime MVP. Whether the CLI and service packages will be merged remains an open specification decision.

## Part 2: Target usage (not implemented)

The `init`, `chat`, `serve` commands and APIs below do not exist yet. Do not execute these steps as a working tutorial; they illustrate the design. Check the [implementation status](https://github.com/hefrankeleyn/oryxos-practice/blob/main/docs/IMPLEMENTATION_STATUS.md) for current progress.

### 1. Initialize a workspace

```bash
# Inject the Provider API key from the environment; never put it in the Agent file

oryxos init                      # create the .oryxos/ workspace (idempotent; never overwrites)
oryxos profile create weather    # scaffold .oryxos/agents/weather/AGENT.md
```

Workspace layout:

```text
.oryxos/
├── agents/            # one sub-directory per Agent (AGENT.md + skills/ + scripts/)
├── skills/            # shared Skill library (SKILL.md + resources)
├── memory/MEMORY.md   # long-term memory
├── sessions/          # role remains open; core sessions target SQLite persistence
├── logs/              # structured logs
├── mcp_servers.yaml   # MCP server configuration
├── AGENTS.md          # bootstrap: project-wide behavior
├── SOUL.md            # bootstrap: default persona
├── USER.md            # bootstrap: user preferences
└── oryxos.db          # SQLite (sessions, audit, schedules)
```

### 2. Define an Agent

Edit `.oryxos/agents/weather/AGENT.md`: **the frontmatter is the runtime configuration; the body is the task instructions.**

```markdown
---
name: weather
description: Check the weather every morning and suggest what to wear
provider:
  name: deepseek
  model: deepseek-chat
tools:
  - http_get
  - notify
schedules:
  - key: morning
    cron: "0 0 8 * * *"
    zone: Asia/Shanghai
    message: Check today's weather in Beijing and suggest what to wear
settings:
  max_iterations: 10
---

You are a friendly weather assistant.
1. Call the weather API for today's weather in Beijing;
2. Suggest what to wear based on temperature, rain and wind;
3. Push the result to the `team-lark` notification channel via notify.
```

::: tip Keep secrets out of files
Never put API keys in `AGENT.md`. Use `${ENV_VAR}` placeholders; they are resolved from environment variables at startup.
:::

### 3. Chat with the Agent

```bash
oryxos chat --profile weather
> What's the weather in Beijing? What should I wear?
```

The Agent runs a ReAct loop, calls `http_get` for the weather data and replies with a suggestion. Every LLM call and tool call is written to the audit tables.

### 4. Run as a service

```bash
oryxos serve   # listens on 8080 and starts the scheduler
```

```bash
# One-shot, stateless invocation
curl -X POST http://localhost:8080/api/v1/agents/weather/invoke \
  -H 'Content-Type: application/json' \
  -d '{"message": "Do I need an umbrella in Shanghai tomorrow?"}'
```

Once `serve` is running, the tasks declared in `schedules` fire automatically — through exactly the same pipeline as the CLI and the REST API.

## Built-in tools

| Tool | Description |
|---|---|
| `read_file` / `write_file` / `list_dir` | File operations, restricted by a path allow-list |
| `shell` | Runs allow-listed commands (argument arrays, no shell interpretation) with a timeout |
| `http_get` / `http_post` | HTTP requests, restricted by a domain allow-list |
| `save_memory` / `recall_memory` | Write to / keyword-search long-term memory |
| `notify` | Push a message to a registered notification channel (WeCom, Feishu, DingTalk webhooks, …) |

## Next steps

- [Architecture](./architecture): how a message flows through OryxOS
- [FAQ](./faq)
