---
title: 常见问题
description: 关于 OryxOS 的定位、技术选型、安全、生产可用性等常见问题。
outline: deep
---

# 常见问题

## 定位

### OryxOS 和 OpenClaw、Hermes Agent 有什么区别？

三者都是 Agent OS，是同类不同定位。OpenClaw（Node.js）偏个人与开发者，Hermes Agent（Python）偏个人到小团队；OryxOS 用 Java 实现，直接定位**严监管企业**场景：私有部署、完全可审计、能纳入企业现有的 Java 技术栈和 IT 治理体系。三者都采用 markdown + frontmatter 的目录形态，社区的优质 Skill 经过审查后理论上可以导入 OryxOS。

### OryxOS 和 Dify、Coze 是竞争关系吗？

不是，是互补关系。Dify、Coze 是工作流编排平台，产物是一条显式的流程图；OryxOS 的产物是一个常驻运行的 Agent，Agent 在运行时根据指令、工具和上下文自己决定下一步。两者可以组合：Dify 作为应用层，调用 OryxOS 的 API。

### OryxOS 和 Spring AI、LangChain4j 有什么区别？

框架给你一组库，让你用代码把 Agent 写出来，运行环境要自己搞定；OryxOS 是一个装好就能跑的服务，业务方不写 Agent 后端代码。两者是复用关系：OryxOS 的 LLM 调用层就是基于 Spring AI Alibaba 实现的。

### OryxOS 会做任务拆解、多 Agent 协作编排吗？

核心阶段不做。OryxOS 守在"运行时"这一层，不往上做可视化编排和复杂任务分解。跨节点的 Agent 协作是[路线图](./roadmap)中阶段三的愿景。

## 使用

### 定义一个 Agent 需要写代码吗？

不需要。在 `.oryxos/agents/<name>/` 放一份 `AGENT.md`：frontmatter 写用哪个模型、能用哪些工具、要不要定时，正文用自然语言写任务。需要调用外部系统时，优先复用社区现成的 MCP server。

### Agent 和 Skill 有什么区别？

**一个目录定义一个 Agent**（`.oryxos/agents/<name>/AGENT.md`），它决定"怎么跑、干什么"。**Skill 是全局共享的能力模板**（`.oryxos/skills/<name>/SKILL.md`），Agent 通过自己目录下的 `skills/` 软链接选择要用哪些。Skill 每轮只把名称和描述注入 prompt，正文由模型按需读取，避免撑爆上下文。

### 支持哪些大模型？

通过 Spring AI Alibaba 对接主流模型：DeepSeek、通义、Kimi、智谱、混元、豆包、Anthropic、OpenAI 等；也可以接入企业自己的本地推理服务（Ollama、vLLM）。OpenAI 兼容协议是事实标准，实现了这套协议的 Provider 都能直接接入。

### 业务系统怎么集成？

通过 REST API。同步调用用 `POST /api/v1/agents/{name}/invoke`；连续对话先创建会话，再多次发消息；告警系统、CI/CD 也可以通过 HTTP 触发 Agent。任何能发 HTTP 请求的语言都能接入。

## 安全与数据

### 数据会发送到外部吗？

OryxOS 本身不收集、不外发任何数据，所有会话、记忆、审计都留在你自己的基础设施上。唯一的出站流量是你配置的 LLM API、MCP server 和通知渠道；如果使用本地推理服务，数据可以完全不出企业。

### 工具调用怎么保证安全？

所有文件、命令、网络访问都要经过沙箱校验：文件路径白名单（防路径穿越）、命令白名单（不经 shell 解释）、HTTP 域名白名单。每次调用（包括被拒绝的）都写入审计表。API Key 等凭证通过环境变量注入，不明文写进配置文件。

### 现在能用于生产环境吗？

还不能。OryxOS 处于阶段一开发中；核心阶段的沙箱是应用层白名单，只能防止模型误操作，不足以隔离蓄意的恶意代码。认证、多租户、容器级隔离都在后续阶段补齐。

## 技术选型

### 为什么选择 Java？

Java 和 Spring 生态在企业后端的每一层都有成熟实现，唯独 Agent OS 这一层是空白。企业的 ERP、CRM、SSO、监控大量是 Java 系统。用 Java 实现，OryxOS 就是一个标准的 Spring Boot 应用：直接调用企业现有的 Java 服务，复用现有运维工具链，走现有的代码审计与合规流程。

### 为什么要自己实现 ReAct 循环？

ReAct 循环是 Agent 的核心，自己实现才能完全掌控循环行为：迭代次数、消息累积、错误处理、审计写入。Spring AI 只用来做 Provider 协议转换和工具 schema 生成，它自带的自动工具执行被禁用，避免工具被调用两次。

### 为什么核心阶段用 SQLite 而不是向量数据库？

为了保持单二进制、装好就跑。会话、审计落 SQLite，长期记忆落 `MEMORY.md` 加关键词检索，先跑通最短链路。记忆接口已经为语义检索预留了升级空间，扩展阶段再引入向量检索。

## 项目

### 开源协议是什么？

Apache License 2.0。

### 怎么参与贡献？

OryxOS 是 oryx-labs 社区的项目，一个由爱好驱动、用 AI coding 构建的 AI 探索社区。欢迎在 [GitHub](https://github.com/hefrankeleyn/oryxos-practice) 提 issue、讨论设计、提交 PR。
