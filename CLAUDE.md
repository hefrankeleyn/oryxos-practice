# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目性质

**OryxOS** 是一个 Java 原生、企业私有部署的 **Agent OS（Agent Harness OS）**：一个底座上运行、管理一群 Agent。它不是某个具体的业务 Agent，也不是 Agent 框架或工作流编排平台。当前是 **greenfield**：9 个 Maven 模块的骨架已就绪（能编译、打包、启动），业务代码尚未实现。本仓库是 oryx-labs 社区的练习实现，用 AI coding（Spec-Kit + Claude Code）驱动开发。

一句话定位：**一个目录 = 一个 Agent**（`.oryxos/agents/<name>/AGENT.md`）。业务方配置 Agent、写 Tool，不写 Agent 后端代码。

## 目录结构

| 路径 | 内容 |
|---|---|
| `docs/01-IndustryResearch.md` | 业界调研：Agent OS 定义、OpenClaw / Hermes 对比、Java 生态缺位、OryxOS 定位与三阶段路线 |
| `docs/02-DemandAnalysis.md` | 需求文档（What）：五大核心能力、核心 / 扩展 / 社区三档功能、非功能需求、数据模型、验收标准 |
| `docs/03-TechnicalSolution.md` | 技术方案（How）：关键技术决策、模块职责、数据持久化、Demo 流程 |
| `docs/04-AiProgrammingGuide.md` | AI 编程指南：Spec-Kit 流程、5 个 user story 拆解、constitution 原则、常见跑偏点 |
| `docs/05-oryx-labs.md` | 社区介绍（背景，不影响实现） |
| `docs/06-oryxos.md` | 项目主页文案：定位、愿景、核心特性、路线图 |
| `docs/prompt/` | 用户自己的执行记录和提示词，不是需求来源 |
| `website/` | 项目主页（VitePress，线上地址 https://oryxos.hifane.com）。中文站在根路径，英文站在 `/en/`；首页是 `.vitepress/theme/components/Home.vue`，页面文案用 `t('中文','English')` 切换。维护说明见 `website/README.md` |
| `docs/images/` | logo 与架构图的源文件。`website/public/images/` 里是复制件，修改后两处都要同步 |

文档是需求和设计的唯一来源。实现前先读对应章节，不要凭印象补设计。

## 本次实施范围（冲突裁决）

03 技术方案混入了大量后续迭代内容（带 `014/015/017/025`、"第 21/28/29 节" 等编号）。**核心阶段只做 02 需求文档 + 03 中标注为"核心阶段"的内容**，文档互相冲突时按下表执行：

| 冲突点 | 本仓库采用 |
|---|---|
| Maven 模块数（9 vs 14） | **9 个**：`oryxos-core`、`oryxos-provider`、`oryxos-memory`、`oryxos-tool`、`oryxos-channel-cli`、`oryxos-web`、`oryxos-storage`、`oryxos-cli`、`oryxos-boot`。persona、knowledge、飞书 / 企微 / 钉钉渠道模块属于扩展阶段 |
| 内置 Tool 数 | **9 个**：`read_file`、`write_file`、`list_dir`、`shell`、`http_get`、`http_post`、`save_memory`、`recall_memory`、`notify` |
| 验收 Demo | 以 **03 第 12 章**为准：每日天气（只有 AGENT.md）、每日科技日报（AGENT.md + 公共 Skill）、每日 GitHub 日报（AGENT.md + scripts/） |
| Skill 注入 | 每轮只注入已绑定 Skill 的 name / description / 本地路径，正文经 `read_file` 按需读取；绑定关系用 Agent 目录下 `skills/<name>` 相对软链接表达，**不用** frontmatter `skills:` |
| Profile 形态 | 不再有单独的 Profile YAML 文件，由 `AgentLoader.deriveProfile()` 从 `AGENT.md` frontmatter 派生 |
| Memory 后端 | 核心阶段只做 `MarkdownMemoryStore`（`MEMORY.md`，分 `## 核心记忆` / `## 归档记忆`，关键词检索），但保留 `LongTermMemoryStore` 接口。SQLite / Mem0 后端、embedding、RRF 融合都属于扩展阶段 |
| CLI 命令 | **12 个**（见下文），`agent import` 属于扩展阶段 |
| 建表方式 | 手工维护建表脚本；不依赖 `ddl-auto=update` 做表结构演进。Flyway 属于扩展阶段 |
| 任务拆解 / 多 Agent 协作（06 的愿景） | 核心阶段**不做**，与 01 §5.4"做运行时，不做编排"保持一致 |

发现新的冲突时，先停下来问用户，不要自行选择。

## 不可违背的架构原则（constitution）

违反以下任何一条都算 bug，不是风格问题：

1. **JDK 21 + Spring Boot 3.x 单体**，Maven 多模块，打成单个可执行 JAR。并发靠 virtual thread，不用响应式编程。
2. **ReAct 循环自己实现**（`ReActLoop` + `PromptBuilder` + `ToolExecutor`），不使用 Spring AI 的 Agent 抽象。
3. **Spring AI 只用一半**：只用 Provider 抽象、协议转换、`@Tool` JSON Schema 生成。**必须禁用 Spring AI 的自动 tool 执行**，否则 tool 会被调用两次。tool 调度完全由 `ToolExecutor` 负责。
4. **Provider 显式映射**：`ProviderService` 维护 provider name → `ChatModel` 映射表，不能靠扫描容器里所有 `ChatModel` Bean 来区分。
5. **Agent 目录不是 Tool**：`AGENT.md` 由 `AgentLoader` / `ContextLoader`（在 core 中）解析，正文注入 system prompt，不注册进 `ToolRegistry`。
6. **Tool 相关代码只放一个 `oryxos-tool` 模块**（内置 Tool、MCP Client、`ToolRegistry`、Sandbox、Notify），不拆模块。所有 Tool 都包装成 `OryxTool`（`getName` / `getDescription` / `getInputSchema` / `execute` → `ToolResult`）。
7. **Memory 走统一门面**：ReAct 只调 `MemoryService`；会话记忆委托给 `SessionManager`，长期记忆委托给 `LongTermMemoryStore`。长期记忆不缓存，核心区永不截断。
8. **Sandbox 接口先行**：`Sandbox.enforce(SandboxAction)` 的签名里不出现"白名单""容器"之类的实现细节。核心阶段只实现 `WhitelistSandbox`（路径需处理 `../` 穿越；shell 只直接执行白名单内的可执行文件和参数数组，不经 shell 解释；HTTP 按域名白名单；SMTP 按 `host:port`）。**不使用 `SecurityManager`**。
9. **审计从第一天就落库**：每次 Tool 调用写 `tool_invocations`，每次 LLM 调用写 `llm_calls`（包括失败和 Sandbox 拒绝）。只写日志不算。
10. **三个触发入口走同一条链路**：CLI、Web Service（人推）和 `AgentScheduler`（钟推）都调用 `AgentService.process(Session, String)`，`ReActLoop` 不感知触发来源。当前 Profile 通过 `ProfileContext`（ThreadLocal，在 `finally` 中清理）传递，不改 `OryxTool.execute` 的签名。
11. **密钥不落地**：frontmatter 和配置中用 `${ENV_VAR}` 占位，由 `ConfigLoader` 解析并校验。
12. **通知渠道是全局注册表**（SQLite `notify_channels`），Agent 正文按名称引用；frontmatter 中**没有** `notify_channels` 字段。

## 核心能力与模块对照

| 能力 | 模块 | 关键类 |
|---|---|---|
| 一、对接 LLM | `oryxos-provider` | `ProviderService`、Function Calling 适配 |
| 二、ReAct 循环 | `oryxos-core` | `ReActLoop`（默认 `max_iterations=10`）、`PromptBuilder`、`ToolExecutor`、`AgentService` |
| 三、Memory | `oryxos-memory` | `MemoryService`、`LongTermMemoryStore` / `MarkdownMemoryStore`、`MemoryTools` |
| 四、Tool | `oryxos-tool` | `OryxTool`、`ToolRegistry`、`FileTools` / `ShellTools` / `HttpTools` / `NotifyTools`、`McpClientService`（先做 stdio）、`McpToolAdapter`、`Sandbox` / `WhitelistSandbox`、`NotifyChannelAdapter` / `WebhookNotifyAdapter` |
| 五、Web Service | `oryxos-web` | 6 个 `*ApiController`、`GlobalExceptionHandler`（统一 `ApiResponse` 信封：code / message / data / timestamp） |
| 支撑 | `oryxos-core` | `AgentLoader`、`ProfileRegistry`、`ContextLoader`、`AgentScheduler`（`ThreadPoolTaskScheduler` + `CronTrigger`，按任务使用 `ReentrantLock` 防止重叠执行） |
| 支撑 | `oryxos-storage` | SQLite + Spring Data JPA：`sessions`、`tool_invocations`、`llm_calls`、`scheduled_tasks`、`task_executions`、`notify_channels` |
| 支撑 | `oryxos-channel-cli` / `oryxos-cli` / `oryxos-boot` | `CliChannel`；Picocli 入口与 `ConfigLoader`；Spring Boot 主类 |

依赖顺序（对应 user story）：US-1 Provider → US-2 ReAct → (US-3 Memory ∥ US-4 Tool) → US-5 Web + 持久化 + CLI 补全。

### PromptBuilder 组装顺序

1. system prompt = `AGENT.md` 正文 + Bootstrap（`AGENTS.md` / `SOUL.md` / `USER.md`）+ 已绑定 Skill 元数据 + **当前日期时间**
2. 长期记忆（`MemoryService`）
3. 对话历史（按 `max_history_turns`（默认 20）截断）
4. 当前 Profile 可用的 Tool 列表（Function Calling 格式）

### 工作区 `.oryxos/`

`agents/`、`skills/`、`memory/MEMORY.md`、`sessions/`、`logs/`、`output/`、`mcp_servers.yaml`、`AGENTS.md`、`SOUL.md`、`USER.md`、`oryxos.db`。`oryxos init` 必须**幂等**，不覆盖已存在的文件。

### CLI（12 个命令）

`init`、`status`、`chat [--profile]`、`serve`（默认端口 8080，同时启动定时调度）、`gateway`、`profile list|create|show|delete`、`provider list`、`tool list`、`session list`。不需要 Spring 上下文的命令（如 `init`、`profile list`）直接操作文件，以保证启动速度。

### REST（核心 10 个，前缀 `/api/v1`）

`POST /sessions`、`POST /sessions/{id}/messages`、`GET /sessions/{id}`、`DELETE /sessions/{id}`、`POST /agents/{name}/invoke`、`GET /profiles`、`GET /memory`、`GET /tools`、`GET /health`、`GET /info`。约束：单条消息 ≤ 32KB，历史最多返回最近 100 条，Agent 调用超时 60 秒返回 504，Provider 故障返回 503。核心阶段不做认证、SSE、限流。

### Session

`session_id` 由 channel + user + profile 联合生成。钟推的 channel 和 user 固定为 `scheduler`。上下文超长时截断早期对话。

## 构建与运行

```bash
mvn clean package                      # 编译 + 测试 + 打包，产物 oryxos-boot/target/oryxos.jar（fat JAR）
mvn clean package -DskipTests          # 跳过测试
mvn -pl oryxos-core -am test           # 单模块测试（-am 连带构建其依赖模块）
mvn -pl oryxos-boot -am test -Dtest=OryxOsApplicationTests#contextLoads   # 单个测试
java -Doryxos.home=/path/to/.oryxos -jar oryxos-boot/target/oryxos.jar     # 启动 Spring 服务（工作区默认 ./.oryxos）
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version    # CLI：打印版本（-h 查看帮助）
java -Doryxos.log.level=DEBUG -jar oryxos-cli/target/oryxos-cli-*-exec.jar  # CLI：打开调试日志（输出到 stderr）
npm run docs:dev                       # 主页本地预览（在仓库根目录执行，首次需要 npm install）
npm run docs:build                     # 构建主页；推送到 main 后由 .github/workflows/deploy-website.yml 发布到 GitHub Pages
```

CLI 约定：入口是 `OryxOsCliLauncher.main`，它只负责指定 CLI 专用日志配置（`oryxos-cli-logback.xml`，默认只输出 WARN 及以上到 stderr，stdout 只留命令结果），然后交给 Picocli 顶层命令 `OryxOsCommand` 执行。**Launcher 不能加 `@Slf4j`**，否则日志配置会失效。子命令以 `subcommands` 的形式挂到 `OryxOsCommand` 下。版本号来自 Maven 过滤后的 `oryxos-version.properties`，因为用的是 Boot parent，占位符分隔符是 `@...@`。

工程约定：

- 坐标 `com.oryxlabs:oryxos-*:0.1.0-SNAPSHOT`，根包 `com.oryxlabs.oryxos`，各模块子包：`core`、`provider`、`memory`、`tool`、`channel.cli`、`web`、`storage`、`cli`；启动类 `OryxOsApplication` 在根包（`oryxos-boot`），组件扫描覆盖全部模块。
- 根 POM 继承 `spring-boot-starter-parent 3.5.9`，并导入 `spring-ai-bom 1.0.3`、`spring-ai-alibaba-bom 1.0.0.4`。第三方版本统一在根 POM 的 `<properties>` / `<dependencyManagement>` 管理，子模块不写版本号。
- 依赖方向：`oryxos-core` 不依赖任何内部模块；其他功能模块只依赖 `core`；`oryxos-boot` 聚合全部模块。实现类依赖 core 中的接口（依赖倒置），模块之间不横向依赖。
- `provider` / `tool` 只引入 `spring-ai-model`（抽象 + `@Tool`），**不引入 Spring AI 的自动配置 starter**。接入具体 Provider（US-1）时再按显式映射原则添加。
- `application.yaml`：virtual thread 已开启；SQLite 数据源为 `${oryxos.home}/oryxos.db`，`ddl-auto: none`。启动冒烟测试使用 `jdbc:sqlite::memory:`。
- Maven 本地仓库和阿里云镜像在用户的 `~/.m2/settings.xml` 中配置。

## 编码规范

- 遵守用户全局规范：**Lombok 能用就用**（`@Data` / `@Builder` / `@RequiredArgsConstructor` / `@Slf4j` 等）；类、字段、方法（参数 / 返回值 / 异常）和关键逻辑都写**详细中文注释**；方法内用 `log` 打**关键日志**（入参、关键分支、失败路径；正常流程用 info / debug，异常用 warn / error）。
- 只使用 JDK 21 特性，注入方式优先构造器注入。
- Spring AI / Spring AI Alibaba / MCP Java SDK 的 API 变动很快，写代码前先确认所用版本的实际 API（`@Tool` 注解名、禁用自动 tool 执行的方式等），不要凭记忆写。
- 核心阶段的原则是"跑通优先于完美"，但上面的架构原则不能为了跑通而破坏。

## Spec-Kit 工作流

- `.specify/memory/constitution.md` 定下后，**AI 不得自行修改**；原则需要调整时停下来和用户讨论。
- `/speckit.plan` 的模块结构必须是上面的 9 个模块。
- 每个 user story 完成后必须运行 `/speckit.analyze`，并用一次 git commit 标记完成。
- 增量阶段（小 feature、修 bug）直接用提示词 + Claude Code，不走完整 Spec-Kit 流程。

## 常见跑偏点（review 时逐条检查）

- 开启了 Spring AI 的自动 tool 执行（症状：tool 被调用两次）
- Provider 靠类型扫描区分
- 把 `AGENT.md` / Skill 当作 Tool 注册
- 把 Tool 拆成多个模块，或把 Memory 与 Session 合并、绕过 `MemoryService`
- 审计只写日志、没有落库
- Prompt 中预载了 Skill 正文
- `shell` 通过 `bash -c` 拼接命令
