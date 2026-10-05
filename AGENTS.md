# AGENTS.md

本文件适用于整个仓库，是 Codex、Claude Code 及其他编码 Agent 共用的工程约束。子目录以后如有更具体的 `AGENTS.md`，只覆盖对应目录，不改变这里的全局原则。

## 项目边界

OryxOS 是 Java 原生、企业私有部署的 Agent OS（Agent Harness OS）。当前仓库处于 **Runtime MVP 工程骨架阶段**：9 个 Maven 模块、Boot 入口、CLI 版本入口和项目网站已存在，Provider、ReAct、Memory、Tool、REST API、SQLite 业务持久化等核心能力尚未实现。

- 开始任务前按需查看 `docs/IMPLEMENTATION_STATUS.md`，不要从 README、官网文案、POM 描述或 `package-info.java` 推断某项能力已经实现。
- “模块存在”“代码实现”“测试通过”“已部署”“用户验收”是不同状态，汇报时必须分开。
- `docs/prompt/` 是用户的执行记录，不是需求或设计来源；除非用户明确要求，否则不要改动。

## 按任务选择资料

不要为一个小改动机械地阅读所有文档，只读取与任务有关的来源：

| 任务 | 主要来源 |
|---|---|
| Agent OS 行业背景、竞品格局、Java 生态与产品定位 | `docs/01-IndustryResearch.md` |
| 产品范围、验收和非功能要求 | `docs/02-DemandAnalysis.md` |
| 架构、接口、模块和关键流程 | `docs/03-TechnicalSolution.md` |
| 主体开发顺序与 Spec-Kit 工作流 | `docs/04-AiProgrammingGuide.md` |
| 对外定位与愿景 | `docs/06-oryxos.md` |
| 当前真实实现状态 | `docs/IMPLEMENTATION_STATUS.md` |
| 网站开发、发布、评论和统计 | `website/README.md` |

用户当前指令优先于仓库文档。已有 constitution 或已采纳 ADR 存在时，其约束优先于普通需求和技术文档；未解决的文档冲突必须向用户说明，不能自行选择后悄悄固化。

## 核心阶段已裁决范围

| 项目 | 当前采用 |
|---|---|
| Maven 模块 | 9 个：`oryxos-core`、`oryxos-provider`、`oryxos-memory`、`oryxos-tool`、`oryxos-channel-cli`、`oryxos-web`、`oryxos-storage`、`oryxos-cli`、`oryxos-boot` |
| 内置 Tool | 9 个：`read_file`、`write_file`、`list_dir`、`shell`、`http_get`、`http_post`、`save_memory`、`recall_memory`、`notify` |
| 验收 Demo | 以 `docs/03-TechnicalSolution.md` 第 12 章的每日天气、每日科技日报、每日 GitHub 日报为准 |
| Agent / Profile | 一个目录定义一个 Agent；`AgentLoader.deriveProfile()` 从 `AGENT.md` frontmatter 派生 Profile，不维护独立 Profile YAML |
| Skill | Agent 目录用 `skills/<name>` 相对软链接绑定；每轮只注入 name、description 和读取路径，正文由 `read_file` 按需读取 |
| Memory | 核心阶段只实现 `MarkdownMemoryStore`，保留 `LongTermMemoryStore` 接口；SQLite / Mem0、embedding、RRF 属于扩展阶段 |
| CLI | 12 个命令；`agent import` 属于扩展阶段 |
| 数据库演进 | 核心阶段维护显式建表脚本，`ddl-auto: none`；Flyway 属于扩展阶段 |
| 多 Agent 协作 | 核心阶段不做任务拆解、团队编排或跨节点协作，只做单机 Runtime MVP |

## 不可违背的架构原则

1. 使用 JDK 21、Spring Boot 3.x、Maven 多模块和同步阻塞模型；并发使用 virtual thread，不引入响应式栈。
2. 自实现 `ReActLoop`、`PromptBuilder` 和 `ToolExecutor`，不使用 Spring AI 的 Agent 抽象。
3. Spring AI 只负责 Provider 抽象、协议转换和 `@Tool` JSON Schema；必须禁用自动 Tool 执行，实际调度只由 `ToolExecutor` 完成。
4. `ProviderService` 显式维护 provider name 到 `ChatModel` 的映射，不依赖 Bean 类型扫描区分 Provider。
5. `AGENT.md` 和 Skill 是上下文来源，不注册为 Tool；Agent 加载与上下文组装属于 `oryxos-core`。
6. 内置 Tool、MCP Client、`ToolRegistry`、Sandbox 和 Notify 都放在 `oryxos-tool`；所有 Tool 统一适配为 `OryxTool`。
7. ReAct 只访问 `MemoryService`；会话与长期记忆分别委托给 `SessionManager` 和 `LongTermMemoryStore`，长期记忆不缓存，核心记忆区不截断。
8. Sandbox 使用实现无关的 `Sandbox.enforce(SandboxAction)` 接口。核心阶段的白名单必须处理路径穿越，Shell 采用可执行文件加参数数组直传，禁止 `bash -c` 拼接。
9. 每次 LLM 和 Tool 调用都要落库审计，包括失败与 Sandbox 拒绝；只写日志不算审计完成。
10. CLI、REST API 和 `AgentScheduler` 都调用 `AgentService.process(Session, String)`；`ReActLoop` 不感知触发来源。
11. 密钥只允许使用 `${ENV_VAR}` 等外部注入方式，不得写入源码、frontmatter、示例、日志或提交历史。
12. 通知渠道保存在 SQLite `notify_channels` 全局注册表中，Agent 正文按名称引用，frontmatter 不增加 `notify_channels` 字段。

## 模块与代码约定

- `oryxos-core` 不依赖其他内部模块；功能模块依赖 core 中的接口；`oryxos-boot` 负责聚合。不要建立功能模块之间的横向循环依赖。
- 根包是 `com.oryxlabs.oryxos`，各模块使用对应子包；Spring Boot 主类保持在根包以覆盖组件扫描。
- 第三方版本在根 POM 的 `<properties>` 或 `<dependencyManagement>` 统一管理，子模块不单独写版本。
- 优先使用构造器注入和合适的 Lombok 注解。公共类型、复杂业务契约与关键安全逻辑写清晰中文 Javadoc；注释解释原因和边界，不复述代码。
- 关键入口、分支和失败路径使用 SLF4J 记录日志，不记录密钥、完整 Prompt、敏感 Tool 参数或未经脱敏的模型响应。
- Spring AI、Spring AI Alibaba 和 MCP Java SDK 版本变化快，涉及其 API 时先核对当前依赖版本的实际接口，不凭记忆编写。

## 构建与验证

本地测试使用可丢弃 fixture，不连接生产系统。按改动范围选择最小充分验证：

```bash
mvn test
mvn clean package
mvn -pl oryxos-core -am test
npm run docs:build
java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version
```

- Java 跨模块改动至少运行相关模块测试；影响打包、启动或依赖时运行 `mvn clean package`。
- 网站内容、配置或主题改动运行 `npm run docs:build`；视觉变更还要实际预览相应桌面和移动页面。
- 测试失败时先确认是否由本次改动导致；不要为通过测试而删除断言、跳过测试或破坏架构原则。

## 工作方式与完成标准

- 修改前检查 `git status`，保留用户已有的未提交改动。只编辑任务涉及的文件，不顺手清理无关内容。
- 未经用户明确要求，不提交、不推送、不部署。需要提交时显式暂存目标文件，禁止使用 `git add .`。
- 新能力按 `US-1 Provider → US-2 ReAct →（US-3 Memory 与 US-4 Tool）→ US-5 Web/持久化/CLI` 的依赖顺序推进。
- `.specify/memory/constitution.md` 创建并确认后，Agent 不得自行修改；原则变化必须先与用户讨论。
- 只有在实现、测试和相应 Demo 证据齐全后，才能把能力标记为“已实现”。能力状态发生变化时，在同一改动中更新 `docs/IMPLEMENTATION_STATUS.md`。
- README 和网站必须使用“已实现 / 开发中 / 规划中 / 愿景”等真实标签，示例若尚不可运行必须明确写成目标用法。
- 完成汇报至少说明：修改文件、验证命令与结果、仍未覆盖的运行或外部验收，以及保留的用户原有改动。

## Review 易错项

- Spring AI 自动执行 Tool，造成重复调用。
- Provider 通过类型扫描而非名称显式映射。
- 把 `AGENT.md` 或 Skill 注册成 Tool，或在 Prompt 中预载全部 Skill 正文。
- 将 Tool 拆成多个内部模块，或让 ReAct 绕过 `MemoryService`。
- 审计只写日志，没有写入 `tool_invocations` / `llm_calls`。
- Shell 通过字符串拼接或 `bash -c` 执行。
- 把应用层白名单宣传成可运行不可信代码的强隔离沙箱。
