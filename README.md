<div align="center">

<img src="docs/images/logo-mark.svg" alt="OryxOS logo" width="72">

# OryxOS

**给 Agent 一个可掌控的运行环境。**

Java 原生的 Agent Harness OS：一个目录定义一个 Agent，一个底座提供共享执行环境。

[项目网站](https://oryxos.hifane.com) · [产品介绍](website/docs/what.md) · [架构设计](website/docs/architecture.md) · [真实实现状态](docs/IMPLEMENTATION_STATUS.md)

</div>

## 先看当前状态

> [!IMPORTANT]
> 当前是 **Runtime MVP 工程骨架阶段**，不是可用的 Agent 平台。可以构建 Maven 工程、启动 Boot 骨架、查看 CLI 版本与帮助；Provider、ReAct、Memory、Tool、REST API 和 SQLite 业务持久化尚未实现。下面的架构与 Agent 定义是目标设计，不代表已经可以执行。

| 范围 | 当前状态 |
|---|---|
| 9 个 Maven 模块、Boot 入口、CLI 版本入口 | 已实现工程骨架；业务运行链路尚未实现 |
| 双语项目网站、文档、评论接入点 | 已实现；Giscus 真实评论验收与线上发布需单独核对 |
| Provider、ReAct、Memory、Tool / MCP、REST | 开发目标，尚未实现 |
| 定时任务、调用审计、业务持久化、三个 Demo | 设计已明确，尚未实现 |
| 多租户、团队编排、分布式协作 | 后续规划，不属于核心阶段 |

以 [IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) 为准。工程可构建、测试通过、已部署和用户验收是不同状态。

## 为什么做一个 Agent OS

一个模型能回答问题，不代表一个 Agent 能持续工作。每个业务项目都要处理相似的问题：消息从哪里进来、上下文如何续、工具由谁执行、失败留下什么记录、结果发往哪里。

OryxOS 希望把这些重复的运行机制收敛为一个 Java 底座：

- **业务方定义职责**：通过 `AGENT.md` 描述任务、可用工具和行为规则。
- **运行时负责执行**：统一接入模型，控制推理循环，管理记忆、调度与审计。
- **企业掌控环境**：在自己的服务器、虚拟机或 Kubernetes 中部署，复用 Java 运维体系。

这不是再造一个聊天窗口，也不是用流程画布替代全部业务逻辑。**先把单机执行内核做实，再逐步补齐 Agent 生命周期与治理能力。**

私有部署意味着工作区与存储可以由你管理；如果配置云端模型，请求仍会发送到相应 Provider，不能据此承诺“数据绝不出域”。

## 目标架构

![OryxOS 目标逻辑架构](docs/images/architecture.svg)

这是逻辑调用图，不是 Maven 依赖图；图中业务组件尚未实现。

```text
CLI / REST / 定时任务
  → AgentService.process(Session, message)
  → PromptBuilder：文件定义 + 记忆 + 历史 + 工具
  → ProviderService：显式 provider name → ChatModel
  → 有工具请求：ToolExecutor → 白名单校验 → 执行 → 结果回填
  → 无工具请求：最终响应
  → 每次 LLM / Tool 调用（包括失败与拒绝）落库审计
```

核心设计边界：

- **自实现执行内核**：`ReActLoop`、`PromptBuilder` 和 `ToolExecutor` 不依赖外部 Agent 抽象；Spring AI 仅做 Provider 协议适配与 Tool JSON Schema，禁用自动工具执行。
- **文件即定义**：一个目录定义一个 Agent，Profile 从 `AGENT.md` frontmatter 派生，不维护独立 Profile YAML。
- **渐进式 Skill**：相对软链接绑定 Skill；每轮注入名称、描述与读取路径，正文通过 `read_file` 按需读取。Agent 定义与 Skill 都不是 Tool。
- **统一记忆门面**：ReAct 只访问 `MemoryService`；会话交给 `SessionManager`，长期记忆交给 `MarkdownMemoryStore`。
- **统一工具接口**：9 个内置 Tool、MCP 工具与 Java `@Tool` 扩展都适配成 `OryxTool`。
- **明确安全边界**：应用层白名单处理文件、命令和网络约束，不是运行恶意代码的强隔离沙箱。Shell 使用可执行文件与参数数组，不拼接 `bash -c`。

内置 Tool 的目标范围：`read_file`、`write_file`、`list_dir`、`shell`、`http_get`、`http_post`、`save_memory`、`recall_memory`、`notify`。

## 现在可以运行什么

### 环境与构建

需要 JDK 21、Maven 3.9+；网站开发还需要 Node.js 与 npm。**构建当前骨架不需要 LLM API Key。**

```bash
git clone https://github.com/hefrankeleyn/oryxos-practice.git
cd oryxos-practice
mvn clean package
```

### CLI 版本与帮助

```bash
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --help
```

目前没有 `init`、`chat`、`serve` 等业务命令。CLI 的 exec JAR 与下面的 Boot JAR 是两个独立产物；最终打包契约仍需规格收口。

### 启动 Boot 骨架

```bash
java -jar oryxos-boot/target/oryxos.jar
```

这只启动 Spring Boot 工程入口与现有配置，不提供 Agent 调用、业务 REST 或健康接口。不要将端口监听等同于 Runtime MVP 可用。

### 网站预览

在仓库根目录执行：

```bash
npm ci
npm run docs:dev
npm run docs:build
```

网站维护、评论和统计配置见 [website/README.md](website/README.md)。首页场景切换仅展示目标定义，不调用模型、不模拟真实执行。

## 目标用法：一个目录定义一个 Agent

> 以下是每日天气验收场景的目标配置，当前还不能运行。没有真实凭证，也不要求现在创建这个目录。

```text
.oryxos/agents/weather/
└── AGENT.md
```

```markdown
---
name: weather
provider:
  name: deepseek
  model: deepseek-chat
tools: [http_get, notify]
schedules:
  - key: morning
    cron: "0 0 8 * * *"
    zone: Asia/Shanghai
    message: 查询北京今天的天气并给出穿搭建议
---

查询北京今天的温度、降水与风力。
给出简短的穿搭和出行建议。
通过 notify 发送到 team-lark 渠道。
```

`team-lark` 是通知渠道名称，连接信息保存在全局 `notify_channels` 注册表中；密钥通过 `${ENV_VAR}` 等外部方式注入，不能写进 Agent 正文。

## 模块地图

| 模块 | 目标职责 |
|---|---|
| `oryxos-core` | 核心接口、Agent 加载、上下文、ReAct 与统一入口 |
| `oryxos-provider` | Provider 名称映射、模型协议适配 |
| `oryxos-memory` | 会话管理与 Markdown 长期记忆 |
| `oryxos-tool` | 内置工具、MCP Client、Registry、Sandbox、Notify |
| `oryxos-channel-cli` | 终端交互渠道 |
| `oryxos-web` | REST API 与统一异常处理 |
| `oryxos-storage` | SQLite 业务持久化与审计 |
| `oryxos-cli` | Picocli 命令入口；当前仅版本与帮助 |
| `oryxos-boot` | Spring Boot 启动与模块聚合 |

`core` 不依赖其他内部模块；能力模块实现 core 的接口，`boot` 负责组装，不建立功能模块间的横向循环依赖。

技术基线：JDK 21、Spring Boot 3.x、Maven、同步阻塞与 virtual thread；Spring AI / Spring AI Alibaba 做模型适配，SQLite 做目标业务存储，Picocli 做命令入口。

## 开发路线

1. **规格与工程保障**：收口文档冲突、可执行包契约、许可证正文、CI 与 Spec-Kit artifacts。
2. **运行内核**：US-1 Provider → US-2 ReAct → US-3 Memory 与 US-4 Tool → US-5 Web / 持久化 / CLI。
3. **验收证据**：每日天气、每日科技日报、每日 GitHub 日报，分别保留可重复运行证据。
4. **企业级扩展**：在可运行内核之上推进治理与扩展，不提前承诺多租户或分布式能力。

完整计划见 [网站路线图](website/docs/roadmap.md)，真实进度见 [实施状态](docs/IMPLEMENTATION_STATUS.md)。

## 文档与贡献

| 要了解什么 | 从这里开始 |
|---|---|
| Agent OS 背景、竞品与 Java 定位 | [行业调研](docs/01-IndustryResearch.md) |
| 产品范围与验收 | [需求分析](docs/02-DemandAnalysis.md) |
| 接口、模块与执行流程 | [技术方案](docs/03-TechnicalSolution.md) |
| 开发顺序与 Spec-Kit | [AI 编程指南](docs/04-AiProgrammingGuide.md) |
| 对外定位与愿景 | [OryxOS 定位](docs/06-oryxos.md) |
| 真实实现与验证状态 | [实施状态](docs/IMPLEMENTATION_STATUS.md) |
| 工程约束 | [AGENTS.md](AGENTS.md) |
| 网站开发与发布 | [网站维护说明](website/README.md) |

参与开发前请先阅读 `AGENTS.md` 与本次任务相关的资料。不要把模块占位或网站文案当成已实现能力；能力变化时同步更新实施状态，并保留相关测试与 Demo 证据。

## 许可证

项目声明采用 **Apache 2.0** 协议；当前根目录的 `LICENSE` 正文尚待补齐。该声明不代表已完成许可证交付。

<div align="center">

Made with ❤️ by hefrankeleyn

</div>
