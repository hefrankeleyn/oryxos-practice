---
title: OryxOS 是什么
description: OryxOS 是 Java 原生的企业级 Agent 操作系统（Agent Harness OS）：一个目录定义一个 Agent，一个底座运行一群 Agent。
outline: deep
---

# OryxOS 是什么

**OryxOS** 是一个开源的 **Agent Harness OS**：装在企业自己的 K8s、虚拟机或物理机上的 Agent 运行底座。它为运维助手、客服助手、知识助手等各类业务 Agent 提供统一的模型接入、推理循环、记忆、工具调用、沙箱与审计能力。

> 一个目录定义一个 Agent，一个底座运行一群 Agent。私有部署，数据不出域。

业务方只做两件事：**写一个 Agent 目录**（一份 `AGENT.md`），**配几个工具**。消息从哪来、LLM 怎么调、上下文怎么续、审计怎么落，都是 OryxOS 的事。

![OryxOS 整体架构](/images/architecture.svg)

## 为什么需要 OryxOS

每家公司都有该交给 Agent 的活，但大多数 Agent 还停在 demo，卡在四道门槛上：

| 门槛 | 现状 | OryxOS 的做法 |
|---|---|---|
| 定义 Agent 要写代码 | 最懂业务的人反而做不了 | 一份 `AGENT.md`，自然语言定义 Agent |
| 云平台要把数据拿走 | 严监管行业合规过不去 | 私有部署，数据留在企业自己的基础设施 |
| 执行是黑盒 | 没审计、没白名单，不敢上生产 | 强制沙箱白名单，每次 LLM / 工具调用落库审计 |
| 跑一群 Agent 很难 | 没有"一群 Agent 的操作系统"这一层 | 一个底座统一管理多个 Agent 的生命周期与共享能力 |

更深一层的判断是：**让 Agent 在生产环境可靠工作，瓶颈通常不在模型本身，而在 Agent 的运行环境。** OryxOS 做的不是又一个 Agent，而是让一群 Agent 可靠运行的底座本身。

## Agent runtime 与 Agent Harness OS

- **Agent runtime**：让单个 Agent 跑起来的执行内核，负责调用模型、执行工具、管理上下文、控制推理循环。
- **Agent Harness OS**：在 runtime 之上，管理一群 Agent：多个 Agent 的生命周期、统一的对外渠道与对内接入、统一的记忆、多租户与治理。

借操作系统打个比方：runtime 像单个进程的执行环境，Agent Harness OS 像管理一群进程、调度资源、提供共享服务的那一层。**runtime 让一个 Agent 跑起来，OryxOS 让一群 Agent 被运行和管理起来。**

北极星公式：

```text
自然语言(AGENT.md) + Memory + Tool + MCP + Skill + 知识库 + Notify = 一个 Agent
```

## 五大核心能力

| 能力 | 说明 |
|---|---|
| **对接 LLM** | Provider 抽象统一对接 DeepSeek、通义、Kimi 等主流模型，Agent 不感知厂商；多 Provider 通过显式映射区分，支持本地推理 |
| **ReAct 循环** | 自己实现的推理引擎：LLM 思考是否调工具 → OryxOS 执行并回填结果 → 继续推理，直到给出答案或达到最大迭代次数 |
| **记忆** | 会话记忆 + 长期记忆两层；长期记忆存在人可读的 `MEMORY.md`，关键词检索，接口预留向量检索升级空间 |
| **工具体系** | 内置文件、Shell、HTTP、记忆、通知等 9 个工具；扩展分三档：零代码 Agent 目录 + MCP、轻代码自写 MCP server、重代码 `@Tool` Bean |
| **对外服务** | 所有能力通过 REST API 暴露，任何开发语言都能通过 HTTP 集成 |

## 核心特性

- 🤖 **一个目录 = 一个 Agent**：包含 `AGENT.md` 的目录就是一个 Agent，不用写代码，多个 Agent 同实例并存
- ☕ **Java 原生**：JDK 21 + Spring Boot 3，单个可执行 JAR 部署，复用现有 Java 运维工具链
- 🔒 **私有可控**：装在企业自己的基础设施上，数据不出域，不锁任何云
- 🛡️ **安全隔离**：工具调用经文件、命令、网络白名单校验，凭证走环境变量不落地，全链路可审计
- 🧠 **自实现 ReAct**：核心推理循环自己实现，不套外部 Agent 框架，机制完全可控
- 🔌 **对接开放标准**：工具用 MCP、协作用 A2A、Agent 目录借鉴 Anthropic Agent Skills 形态
- ⏰ **定时自动运行**：Agent 可按 cron 到点自动执行，并把结果推送到企业 IM
- 🌐 **无状态可扩展**：运行实例无状态、状态外置，从架构起为走向分布式留好路

## 设计原则

- **底座优先于 Agent**：最重要的交付不是某个强大的 Agent，而是让任意 Agent 都能可靠运行的环境
- **自实现核心，可控优先**：核心推理循环自己实现，底层模型协议适配复用成熟库
- **配置即 Agent**：一个 Agent 由一份配置定义，而不是由代码写出
- **对接开放标准**：与生态协同，不另立协议
- **无状态实例，状态外置**：从单机平滑走向分布式的前提
- **安全是地基不是补丁**：来源受控、最小权限、强制沙箱、凭证不落地、全链路审计
- **分阶段克制**：先把单机运行时内核做扎实，每次架构升级都用真实使用数据证明其必要性

## 与相关项目的关系

| 类型 | 代表 | 关系 |
|---|---|---|
| Agent OS | OpenClaw、Hermes Agent | **同类不同定位**：它们偏个人与小团队，OryxOS 定位严监管企业 |
| 编排平台 | Dify、Coze | **互补**：编排平台可以作为应用层调用 OryxOS 的 API |
| Agent 框架 | Spring AI、LangChain4j | **复用**：OryxOS 的 LLM 调用层基于 Spring AI Alibaba 实现 |

一句话：框架给你材料让你自己盖房子，编排平台编排的是流程，OryxOS 给你一个拎包入住、可治理、可审计的 Agent 运行底座。

## 下一步

- [快速开始](./quick-start)：从源码构建并了解 1.0 的使用方式
- [系统架构](./architecture)：分层设计、关键技术决策与模块划分
- [路线图](./roadmap)：从单机内核到分布式 Agent 协作
