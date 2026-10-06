---
title: 快速开始
description: 从源码构建 OryxOS，并了解 1.0 版本定义 Agent、对话和以服务方式运行的方式。
outline: deep
---

# 快速开始

::: warning 开发中
OryxOS 正处于**阶段一：单机运行时内核**的开发中。本页第一部分是**现在就能跑通**的构建步骤；第二部分是 **1.0 的目标用法**，命令和接口在正式发布前可能调整。
:::

## 环境要求

- **JDK 21+**
- **Maven 3.9+**
- 操作系统：Linux / macOS

构建和运行当前 CLI 版本入口不需要 LLM API Key。模型配置属于尚未实现的目标运行链路。

## 一、从源码构建（现在可用）

```bash
git clone https://github.com/hefrankeleyn/oryxos-practice.git
cd oryxos-practice
mvn clean package
```

构建会产出两个可执行 JAR：

| 产物 | 用途 |
|---|---|
| `oryxos-boot/target/oryxos.jar` | Spring Boot 工程入口；暂无业务 REST API |
| `oryxos-cli/target/oryxos-cli-<version>-exec.jar` | CLI 入口；目前只有版本、帮助与无参提示 |

验证命令行可用：

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

也可以启动 Boot 工程骨架：

```bash
java -jar oryxos-boot/target/oryxos.jar
```

这不提供 Agent 调用、业务 REST 或健康接口；端口监听不代表 Runtime MVP 可用。最终是否合并 CLI 与服务包仍待规格收口。

## 二、目标用法（尚未实现）

以下 `init`、`chat`、`serve` 等命令与接口目前不存在，不要直接照做。示例用于理解设计；最新状态见 [实施状态](https://github.com/hefrankeleyn/oryxos-practice/blob/main/docs/IMPLEMENTATION_STATUS.md)。

### 1. 初始化工作区

```bash
# 目标运行时启动前从环境注入 Provider API Key，不写入 Agent 文件

oryxos init                      # 在当前目录创建 .oryxos/ 工作区（幂等，不覆盖已有文件）
oryxos profile create weather    # 生成 .oryxos/agents/weather/AGENT.md 模板
```

工作区结构：

```text
.oryxos/
├── agents/            # 每个子目录 = 一个 Agent（AGENT.md + skills/ + scripts/）
├── skills/            # 公共 Skill 库（SKILL.md + 附属资源）
├── memory/MEMORY.md   # 长期记忆
├── sessions/          # 目录职责待收口；核心会话持久化目标为 SQLite
├── logs/              # 结构化日志
├── mcp_servers.yaml   # MCP server 配置
├── AGENTS.md          # Bootstrap：项目级行为说明
├── SOUL.md            # Bootstrap：默认人格
├── USER.md            # Bootstrap：用户偏好
└── oryxos.db          # SQLite（会话、审计、定时任务）
```

### 2. 定义一个 Agent

编辑 `.oryxos/agents/weather/AGENT.md`：**frontmatter 是运行配置，正文是任务指令**。

```markdown
---
name: weather
description: 每天早上查询天气并给出穿搭建议
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
    message: 查询北京今天的天气并给出穿搭建议
settings:
  max_iterations: 10
---

你是一个贴心的天气助手。
1. 调用天气 API 获取北京今天的天气；
2. 根据温度、降水和风力给出简洁的穿搭建议；
3. 通过 notify 推送到 `team-lark` 通知渠道。
```

::: tip 密钥不落地
API Key 等敏感信息不要写进 `AGENT.md`，统一用 `${ENV_VAR}` 占位，启动时从环境变量解析。
:::

### 3. 和 Agent 对话

```bash
oryxos chat --profile weather
> 查一下北京天气，今天穿什么？
```

Agent 会通过 ReAct 循环调用 `http_get` 拉取天气数据，再给出穿搭建议。每次 LLM 调用和工具调用都会写入审计表。

### 4. 以服务方式运行

```bash
oryxos serve   # 默认监听 8080，同时启动定时调度
```

```bash
# 无状态调用一次 Agent
curl -X POST http://localhost:8080/api/v1/agents/weather/invoke \
  -H 'Content-Type: application/json' \
  -d '{"message": "明天上海要带伞吗？"}'
```

`serve` 启动后，`schedules` 里声明的定时任务会到点自动触发，走的是和 CLI、REST API 完全相同的链路。

## 内置工具

| 工具 | 说明 |
|---|---|
| `read_file` / `write_file` / `list_dir` | 文件操作，受路径白名单约束 |
| `shell` | 执行白名单内的命令（参数数组直传，不经 shell 解释），带超时 |
| `http_get` / `http_post` | HTTP 请求，受域名白名单约束 |
| `save_memory` / `recall_memory` | 写入 / 关键词检索长期记忆 |
| `notify` | 推送消息到已注册的通知渠道（企业微信 / 飞书 / 钉钉 webhook 等） |

## 下一步

- [系统架构](./architecture)：了解一次消息在 OryxOS 内部怎么流转
- [常见问题](./faq)
