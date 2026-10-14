# OryxOS Constitution

## Core Principles

### I. Java 工程与同步执行模型

- 必须采用 JDK 21、Spring Boot 3.x 和 Maven 多模块工程；核心阶段保持下文列出的 9 个模块。
- 必须采用同步阻塞模型，HTTP 服务使用 Spring MVC，并发使用 Java 21 virtual thread；
  不引入响应式栈。消息处理、LLM 调用、Tool 执行到最终返回遵循同一同步调用链。
- `oryxos-core` 不依赖其他内部模块；功能模块依赖 core 中的接口，`oryxos-boot` 负责聚合。
  不得建立功能模块之间的横向循环依赖。
- 第三方版本必须在根 POM 的 `<properties>` 或 `<dependencyManagement>` 统一管理，
  子模块不得单独声明版本。
- 根包为 `com.oryxlabs.oryxos`，模块使用对应子包；Spring Boot 主类保留在根包。

理由：沿用仓库已裁决的 Java 原生单机 Runtime 方案，以同步模型保持调用链和调试路径清晰，
通过接口与模块边界维持核心独立性。

### II. 自主管理的 Runtime 与统一入口

- 必须自实现 `ReActLoop`、`PromptBuilder` 和 `ToolExecutor`；不得使用 Spring AI 的 Agent 抽象。
- Spring AI 仅负责 Provider 抽象、协议转换和 `@Tool` JSON Schema 生成。
  必须禁用 Spring AI 自动 Tool 执行；Tool 的实际调度与执行只由 `ToolExecutor` 完成。
- `ProviderService` 必须显式维护 provider name 到 `ChatModel` 的映射，
  不得依赖 Bean 类型扫描区分 Provider。
- CLI、REST API 和 `AgentScheduler` 必须统一调用 `AgentService.process(Session, String)`；
  `ReActLoop` 不得感知触发来源。

理由：循环控制和工具执行权集中在 OryxOS，避免框架自动执行造成重复调用，
同时保证多 Provider 选择与不同入口的处理行为一致。

### III. 上下文、Memory 与 Tool 职责边界

- 一个目录定义一个 Agent；`AgentLoader.deriveProfile()` 从 `AGENT.md` frontmatter 派生 Profile，
  不维护独立 Profile YAML。
- `AGENT.md` 和 Skill 是上下文来源，不得注册为 Tool；Agent 加载与上下文组装归 `oryxos-core`。
- Agent 目录通过 `skills/<name>` 相对软链接绑定 Skill；每轮只注入 name、description 和读取路径，
  Skill 正文由模型通过 `read_file` 按需读取，不得预载全部 Skill 正文。
- ReAct 只通过 `MemoryService` 访问记忆；会话记忆委托给 `SessionManager`，
  长期记忆委托给 `LongTermMemoryStore`。长期记忆每次读取，不做缓存；核心记忆区不得截断。
- 核心阶段只实现 `MarkdownMemoryStore`，保留 `LongTermMemoryStore` 后端接口。
- 内置 Tool、MCP Client、`ToolRegistry`、Sandbox 和 Notify 必须归 `oryxos-tool`；
  内置 Tool、Java Plugin Tool 与 MCP Tool 统一适配为 `OryxTool`。
- 通知渠道保存在 SQLite `notify_channels` 全局注册表中，Agent 正文按名称引用；
  frontmatter 不得增加 `notify_channels` 字段。

理由：上下文、记忆访问和执行能力分别遵守已裁决的接口与模块边界，
避免把声明性 Agent 定义变成执行工具，也避免 ReAct 直接绑定记忆后端。

### IV. 安全边界与密钥外部注入

- 密钥只允许通过 `${ENV_VAR}` 等外部注入方式提供；不得写入源码、frontmatter、示例、
  日志或提交历史。配置加载必须执行必填项与格式的基础校验，缺失或非法时给出清晰报错。
- Sandbox 必须使用实现无关的 `Sandbox.enforce(SandboxAction)` 接口；
  核心阶段采用 `WhitelistSandbox`，在实际 IO 前执行文件路径、Shell 可执行文件与 HTTP 域名校验。
- 文件路径白名单必须处理路径穿越；Shell 必须以可执行文件加参数数组直传，
  禁止字符串拼接与 `bash -c` 执行。
- 不得将应用层白名单描述为可运行不可信代码的强隔离沙箱。
- 关键入口、分支和失败路径必须使用 SLF4J 记录日志；不得记录密钥、完整 Prompt、
  敏感 Tool 参数或未经脱敏的模型响应。

理由：密钥加载和执行前校验是核心阶段已明确的安全边界，
其实现与对外说明必须保持一致。

### V. LLM 与 Tool 调用审计落库

- 核心阶段必须将每次 LLM 调用写入 SQLite `llm_calls`，
  将每次 Tool 调用写入 SQLite `tool_invocations`。
- 审计必须覆盖成功、调用失败以及 Tool 的 Sandbox 拒绝路径；
  CLI、REST API 与定时触发都遵守同一要求。
- LLM 调用记录包含 Provider、模型、token 使用量和耗时；
  Tool 调用记录必须能够表达成功或失败及失败信息。审计内容必须遵守密钥与敏感信息限制。
- 只写应用日志不算审计完成；审计查询接口与报表属于扩展阶段，
  不得以其尚未实现为由推迟核心阶段的审计写入。
- 核心阶段维护显式建表脚本，配置 `ddl-auto: none`；Flyway 属于扩展阶段。

理由：可审计的数据基础从核心阶段建立，避免后续依赖日志反解析补建调用记录。

## 核心阶段范围与来源裁决

本 constitution 只固化 `AGENTS.md`、`docs/02-DemandAnalysis.md` 和
`docs/03-TechnicalSolution.md` 中已经明确裁决的原则。
核心阶段交付单机 Runtime MVP；多租户、SSO、完整 Tool Policy、强隔离沙箱、
任务拆解、团队编排及跨节点协作不属于该阶段。

核心阶段的 Maven 模块固定为：

| 模块 | 职责边界 |
|------|----------|
| `oryxos-core` | 核心接口、Agent 加载、上下文组装、ReAct 与统一处理入口 |
| `oryxos-provider` | Provider 适配与显式模型映射 |
| `oryxos-memory` | Memory 统一门面与长期记忆后端 |
| `oryxos-tool` | 内置 Tool、MCP、注册表、Sandbox 与 Notify |
| `oryxos-channel-cli` | CLI 消息接入 |
| `oryxos-web` | Spring MVC REST API |
| `oryxos-storage` | SQLite 业务与审计持久化 |
| `oryxos-cli` | 命令行入口与配置加载 |
| `oryxos-boot` | Spring Boot 启动与依赖聚合 |

内置 Tool 固定为 9 个：`read_file`、`write_file`、`list_dir`、`shell`、`http_get`、
`http_post`、`save_memory`、`recall_memory`、`notify`。
CLI 核心阶段为 12 个命令，`agent import` 属于扩展阶段。
SQLite / Mem0 长期记忆后端、embedding 和 RRF 属于扩展阶段。

来源中存在不同阶段的描述，按用户当前指令及 `AGENTS.md` 的明确裁决采用以下口径：

- 技术方案第 10 章的 14 模块及 13 个 CLI 子命令描述不改变核心阶段的 9 模块和 12 命令边界。
- 技术方案第 5 章的多 Memory 后端及检索演进不改变核心阶段只实现 `MarkdownMemoryStore` 的裁决。
- 技术方案中的 Flyway 演进不改变核心阶段显式建表脚本与 `ddl-auto: none` 的裁决。
- 内置 Tool 的模块归属采用 `AGENTS.md`；技术方案第 6.2 节中 MemoryTools 归 Memory 的描述
  不改变内置 Tool 统一归 `oryxos-tool` 的裁决。
- 验收 Demo 采用技术方案第 12 章的每日天气、每日科技日报、每日 GitHub 日报；
  需求文档第 13 章的两个 Demo 描述按该已裁决口径读取。
- Agent / Profile 采用 frontmatter 派生；来源中残留的独立 Profile YAML 描述不构成新增要求。

这些规则是实施约束，不是能力完成声明；当前真实状态以 `docs/IMPLEMENTATION_STATUS.md`
及相应实现、测试和 Demo 证据核对。

## 开发流程与最小充分验证

- 修改前必须检查 `git status`，保留用户已有未提交改动，只编辑任务涉及的文件。
  `docs/prompt/` 是用户执行记录，不得作为需求或设计来源；未经明确要求不得修改。
- 新能力按 `US-1 Provider → US-2 ReAct →（US-3 Memory 与 US-4 Tool）→ US-5 Web/持久化/CLI`
  的依赖顺序推进。
- 涉及 Spring AI、Spring AI Alibaba 或 MCP Java SDK API 时，必须先核对当前依赖版本的实际接口。
- 公共类型、复杂业务契约和关键安全逻辑必须写清晰中文 Javadoc，解释原因和边界。
- 本地测试必须使用可丢弃 fixture，不连接生产系统；按改动范围选择最小充分验证：

| 改动范围 | 验证要求 |
|----------|----------|
| Java 跨模块改动 | 至少运行相关模块测试，例如 `mvn -pl oryxos-core -am test`；全量测试使用 `mvn test` |
| 打包、启动或依赖改动 | 运行 `mvn clean package` |
| CLI 版本入口核验 | 使用 `java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version` |
| 网站内容、配置或主题改动 | 在 `website/` 运行 `npm run docs:build` |
| 网站视觉改动 | 在构建之外实际预览相应桌面与移动页面 |

- 测试失败必须先确认是否由本次改动导致；不得为通过测试删除断言、跳过测试或破坏架构原则。
- 核心功能验收按需求文档第 13 章覆盖每个功能模块至少一个端到端测试用例，
  场景验收采用技术方案第 12 章已裁决的三个 Demo。
- 只有实现、相关测试和相应 Demo 证据齐全后，才能标记能力为“已实现”；
  能力状态变化时必须在同一改动中更新 `docs/IMPLEMENTATION_STATUS.md`。
- 必须分别报告模块存在、代码实现、测试通过、部署与用户验收状态。
  README 和网站使用“已实现 / 开发中 / 规划中 / 愿景”真实标签；不可运行的示例标为目标用法。
- 未经用户明确要求，不提交、不推送、不部署；需要提交时显式暂存目标文件，禁止 `git add .`。
- 完成汇报必须说明修改文件、验证命令及结果、未覆盖的运行或外部验收，以及保留的用户原有改动。

## Governance

用户当前指令优先于仓库文档。经确认的 constitution 与已采纳 ADR 的约束优先于普通需求和技术文档；
未解决的来源冲突必须向用户说明，不得自行选择后固化。`AGENTS.md` 提供日常工程约束，
需求和技术方案提供相应范围、验收及设计依据。

constitution 创建并经用户确认后，Agent 不得自行修改。原则变化必须先与用户讨论并获得确认；
修订必须说明变更内容、来源依据及对现有设计和实现的影响，保留仍适用的已采纳决定。
Spec、Plan、Tasks 和代码评审必须核对本 constitution 的阶段边界、模块依赖、
Tool 执行权、安全、审计与验证要求。

版本按语义化版本管理：移除或不兼容地重定义原则升 MAJOR；新增原则或实质扩展治理内容升 MINOR；
不改变语义的澄清、措辞与错字修正升 PATCH。首次建立版本为 `1.0.0`。
首次采纳日期填写用户确认日期；后续修订保持该日期，只更新最后修订日期，日期使用 `YYYY-MM-DD`。
修订时在文件顶部记录 Sync Impact Report，列明版本变化、原则与章节变化及待办；
该报告是临时审阅材料，在提交前移除。

**Version**: 1.0.0 | **Ratified**: 2026-10-14 | **Last Amended**: 2026-10-14
