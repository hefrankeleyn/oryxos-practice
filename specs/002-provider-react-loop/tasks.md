---
description: "Provider 抽象与 ReAct 循环的依赖有序实施任务，仅生成清单"
---

# Tasks: Provider 抽象与 ReAct 循环

**Input**: `specs/002-provider-react-loop/` 中已确认规格与 Plan 设计产物。
**Prerequisites**: [spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、[data-model.md](data-model.md)、[CLI 契约](contracts/cli.md)、[Runtime 契约](contracts/runtime.md)、[验证指南](quickstart.md)、[宪章](../../.specify/memory/constitution.md)。
**Created**: 2026-10-29
**Status**: Implement 已完成可控实现与验证，65/66 项已完成；T065 因缺少真实验收环境变量未执行，本功能尚未全部验收完成。当前 Git 分支为 main，未提交、推送或部署。
**Tests**: 规格明确要求隔离验证、计数观察、持久审计和真实 Provider 验收，因此列出必要的行为、协议与集成测试；不新增测试框架或无关镜像测试。
**Organization**: 四个故事均 P1，按实际依赖先安排 US2 Provider，再 US1 CLI/会话、US3 工具闭环、US4 完整审计验收。US 编号对应本功能 spec，不等同于全局路线图 US-1～US-5。

## Format: `[ID] [P?] [Story] Description`

所有任务使用未勾选复选框、连续 T 编号及准确文件路径；故事阶段使用 [US1]～[US4]。
[P] 仅表示同一前置门槛满足后文件不重叠的并行机会，不是授权多 Agent 产品能力，也不自动启动并行 Agent。
本清单只拆已确认需求，不重新初始化、升级依赖或引入额外产品文案、默认人格、天气/MCP 工具。
所有路径相对仓库根；执行时先确认工作目录。公共类型和关键安全逻辑遵守中文 Javadoc 约定。

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 复用现有 9 模块骨架，补齐本功能依赖与保护验证。

- [X] T001 核对并在 `pom.xml`、`oryxos-provider/pom.xml` 中接入根 BOM 管理的普通 spring-ai-openai；保持 JDK 21、Boot 3.5.9、Spring AI 1.0.3、Alibaba BOM 1.0.0.4、Picocli 4.7.7，不加 starter、不升级；解析后核对 research.md 的准确接口，记录传递流式类依赖但不启用响应式运行栈。
- [X] T002 核对 `oryxos-cli/src/test/java/com/oryxlabs/oryxos/cli/OryxOsCommandTest.java`、`oryxos-boot/src/test/java/com/oryxlabs/oryxos/OryxOsApplicationTests.java` 和各模块 `pom.xml` 的既有测试基线及临时 SQLite 配置，保留原断言；准备只用已有 JUnit/AssertJ/JDK HTTP fixture 的验证，不连接生产、不将测试工具打进主产物。

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 公共契约和真实审计基础必须先可用，不能先放行业务调用、以后再补落库。下列类型均按 plan.md 的目录归属。

- [X] T003 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/ProfileSnapshot.java` 和 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/AgentDefinition.java` 建立配置模型并逐项落实 data-model §1 原文约束：name“必填，等于 agents 下目录名；禁止路径分隔符、`.`、`..`；重复名拒绝”；providerName/model“必填非空；providerName 必须存在于显式配置映射中；不推断厂商”；temperature“可选，有限数值；供应商范围按所选适配器验证，不统一假设所有模型支持”；tools“工具名集合，不能重复；只引用已注册并对当前 Agent 开放的工具”；bootstrap“工作区内文件引用列表，启动时校验存在与真实路径边界，顺序保留”；maxIterations“正整数，默认 10；一次模型调用计一次迭代”；maxHistoryTurns“正整数，默认 20；只限制模型输入中的已完成历史轮数”；identity.prompt“已有 frontmatter 的可选指令；不生成新 persona 或昵称”；body/bootstrap content“正文及引导内容不进入配置快照，每次模型调用前重新读取”。
- [X] T004 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/SessionKey.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/Session.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/Turn.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/model/Message.java` 实现 data-model §3 的公共数据结构：SessionKey“channel、userId、agentName 的结构化三元组，禁止用不带边界的字符串拼接造成碰撞”；Session“sessionId=随机 UUID，会话实例唯一；同一进程用 SessionKey 找当前会话；重启不恢复”；Run/Turn“runId=UUID；对应一次用户输入；状态 RUNNING、SUCCEEDED、FAILED、LIMIT_REACHED；带创建/结束时间”；Message“messageId、turnId、顺序号、role、内容；role 为 USER、ASSISTANT、TOOL；系统上下文另行构建”；assistant 请求“保留模型 toolCallId、工具名及参数；与产生它的 llmCallId 关联”，工具结果“以相同 toolCallId 回填，包含成功/失败和受控错误，不伪造成功”。
- [X] T005 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/provider/ProviderService.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/provider/ProviderRequest.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/provider/ProviderResponse.java` 及 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/memory/MemoryService.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/memory/SessionManager.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/memory/LongTermMemoryStore.java` 定义供应商无关端口，使用 T003/T004 类型；保留 token 未知与工具请求 ID，ReAct 只通过 MemoryService 访问历史，本功能不实现长期记忆后端。
- [X] T006 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/tool/OryxTool.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/tool/ToolCatalog.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/tool/ToolInvoker.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/tool/ToolResult.java` 定义名称、描述、schema、请求/结果端口，区分元数据与执行；允许受控校验失败和 retryable 信息，不绑定 MCP、JPA、Spring AI 执行器或产品测试工具。
- [X] T007 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/audit/AuditStore.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/audit/InvocationAudit.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/audit/AuditPersistenceException.java` 定义同步 begin/complete 契约和安全 DTO；状态约束原文为“PENDING、SUCCEEDED、FAILED；工具另有 REJECTED”，审计不可用是致命处理错误，不能转换成普通 ToolResult；不暴露实体或凭证。
- [X] T008 [P] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/RuntimeStarter.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/RuntimeHandle.java` 定义 CLI 可调用的启动/关闭端口，句柄只暴露 core 契约，Boot 提供实现；不让 cli 引用 boot、memory/provider/storage 实现或 Spring AI 模型类型。
- [X] T009 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/RuntimeFailure.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/SafeErrorMapper.java` 定义 Runtime 契约 §5 的受控错误类别与允许字段摘要；禁止原始 cause/Prompt/工具参数/响应直接序列化，先制定最小可定位信息，精确用户可见文案在实现前审阅，不自行增加产品提示。
- [X] T010 在 `oryxos-storage/src/test/java/com/oryxlabs/oryxos/storage/AuditStoreContractTest.java` 先写 begin 已提交才允许动作、终态实际提交后才返回、拒绝 executed=false、未知用量 NULL、异常结果不确定不伪报的失败用例，临时 SQLite 不建 sessions；实现 T011～T014 后运行验证，不以跳过或删断言通过。
- [X] T011 [P] 在 `oryxos-storage/src/main/resources/db/schema/audit-schema.sql` 维护两表显式幂等 DDL，路径为 `${oryxos.home}/oryxos.db` 且无 sessions 外键；逐项落实 data-model §5 原文：id“TEXT UUID，主键，调用前生成”，session_id/profile_name/run_id“非空关联快照，不保存用户输入内容”，iteration“INTEGER，1 至当前快照的 maxIterations”，success“nullable BOOLEAN；PENDING 为 NULL，成功 true，已确认失败/拒绝 false”，started_at“非空 UTC 时间”，completed_at/duration_ms“PENDING 可空；终态必填；耗时用单调时钟计算且非负”，error_code/error_message“成功为空；失败为受控错误码和脱敏摘要，不直接使用原始异常消息”；不得引入 Flyway、ddl-auto update 或其他业务表。
- [X] T012 [P] 在 `oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/LlmCallEntity.java`、`oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/ToolInvocationEntity.java`、`oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/LlmCallRepository.java`、`oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/ToolInvocationRepository.java` 映射 T011 固定字段契约；专属字段原文：“provider、model 必填；prompt_tokens、completion_tokens、total_tokens 为 nullable 非负整数”，“llm_call_id、tool_call_id、tool_name 必填，executed 为 nullable BOOLEAN”，“PENDING=NULL（不能由此断言未执行），SUCCEEDED=true；校验拒绝 REJECTED=false；执行后失败 FAILED=true”，“唯一键 `(run_id, llm_call_id, tool_call_id)`，不保存原始 input_json/result_json”；不建立到内存 Session 的实体关系。
- [X] T013 在 `oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/JpaAuditStore.java` 实现 begin/complete 独立短事务及提交确认，IO 留在事务外，终态以 PENDING 为更新前置条件；提交前失败保留 PENDING，提交结果未知停止并只读核实，不倒退已提交终态、不重放；不以 save/flush 等同 commit，不提供日志降级或异步队列。
- [X] T014 在 `oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/AuditSchemaInitializer.java` 和 `oryxos-boot/src/main/resources/application.yaml` 接入显式 schema 初始化与兼容性校验，保持 ddl-auto: none；初始化在任何模型/工具调用前完成，已有 schema 不兼容时失败，不自动迁移或清空数据。
- [X] T015 在 `oryxos-storage/src/test/java/com/oryxlabs/oryxos/storage/AuditSchemaInitializationTest.java` 验证首次建表、再次初始化保留记录、约束生效及不兼容/不可写故障，确认只存在本功能两张业务审计表；不把 sqlite 内部表算作新增业务能力。
- [X] T016 运行 T010/T015 对应 `oryxos-storage/pom.xml` 的相关模块测试，并核对 `oryxos-core/pom.xml` 仍无内部功能模块依赖；只有真实审计提交与失败传播通过，才放行后续调用集成。保存后续执行证据，不在此将占位模块标为已实现。

**Checkpoint**: 公共类型、端口和持久审计可用；所有后续故事均必须使用它们。此阶段尚无真实模型或产品工具调用。

## Phase 3: User Story 2 - 配置选择 Provider/模型并定位错误 (Priority: P1)

**Goal**: Provider 先于实际 ReAct/CLI 装配完成；两个可控目标选择、错误脱敏和协议适配可独立验证。
**Independent Test**: 两个回环目标、缺失凭证、超时、模型失败及返回工具请求均通过协议测试；真实 DeepSeek 的完整故事验收依赖 US3 后的 T065，不把本阶段可控验证当真实验收。

### Tests for User Story 2

- [X] T017 [P] [US2] 在 `oryxos-provider/src/test/java/com/oryxlabs/oryxos/provider/ProviderSelectionTest.java` 定义两实例同类型 ChatModel 显式映射、模型选择、未知服务/缺失密钥零请求及错误不泄露凭证的失败用例，对应 FR-005～007、SC-003/006，不依赖 CLI 或真实服务。
- [X] T018 [P] [US2] 在 `oryxos-provider/src/test/java/com/oryxlabs/oryxos/provider/DeepSeekProtocolTest.java` 使用 JDK 回环 HTTP fixture 验证 POST 路径、thinking.disabled、模型/消息/schema/tool ID 保持、Content-Length、HTTP 只尝试一次、延迟响应有限超时、callback 执行入口零调用；先写失败行为再实现适配，不以当前网页示例替代本地协议验证。

### Implementation for User Story 2

- [X] T019 [US2] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/provider/ProviderDefinition.java`、`oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/ConfigLoader.java` 实现外部配置及环境占位符校验，落实原文“providerName 为唯一非空键，映射到显式创建的 ChatModel；相同类型模型可以有多个具名实例”，“model 非空且由 Agent 选择；API key 为外部环境变量引用，解析后的值仅用于内存客户端装配”，“baseUrl 必须是无 userinfo、无凭证 query 的 HTTP(S) URL”；错误定位字段不回显值，DeepSeek 模型来自 DEEPSEEK_MODEL，不硬编码旧别名。
- [X] T020 [US2] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/ProviderModelConfiguration.java` 显式建立 providerName→ChatModel，使用普通 OpenAiApi/OpenAiChatModel；默认与每次调用都 internalToolExecutionEnabled(false)，资格谓词固定 false；RetryTemplate maxAttempts=1/noBackoff，不扫描 Bean 类型区分厂商，不启用自动执行/回退/流式。
- [X] T021 [US2] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/DeepSeekRequestInterceptor.java` 仅对 DeepSeek 实例匹配的 POST chat-completions JSON 补入 thinking.type=disabled，更新 Content-Length，保留 messages/tools/调用 ID，执行请求恰好一次；不打印 header/body，不为其他 Provider 注入 DeepSeek 参数。
- [X] T022 [US2] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/SpringAiProviderService.java` 映射 core Prompt/响应与 Spring AI 1.0.3 消息，保留 assistant 原工具请求和匹配的 ToolResponseMessage；无工具且无文本返回协议失败；用量缺失为 NULL，不将工具结果改装成用户输入。
- [X] T023 [US2] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/provider/AuditedProviderService.java` 装饰实际调用端口：begin 已提交才调用，成功/失败/协议错误均提交安全审计，complete 失败直接终止本次处理；每次真实 HTTP 尝试对应一次审计，不因异常捕获再次完成或重发。
- [X] T024 [US2] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/ProviderModelConfiguration.java` 通过 JDK HttpClient.connectTimeout 与 JdkClientHttpRequestFactory.setReadTimeout 设置正时长 connect-timeout/read-timeout，默认 10s/60s、重启生效；不得无限等待、增加自动重试或将参数宣称为任务级 SLA，扩展 T018 验证配置覆盖与超时后一次失败审计。
- [X] T025 [US2] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/ProviderFailureMapper.java` 统一处理鉴权拒绝、不可达、超时、非法响应，接入 T009 安全摘要；不得将原始响应体/Authorization/cause 输出终端或日志，不猜测 Provider/Fallback，不用零冒充未知 token。
- [X] T026 [US2] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditedProviderIntegrationTest.java` 用最小测试装配显式连接 T019～T025 的 Provider、T023 审计装饰器与 Phase 2 的真实临时 SQLite/JpaAuditStore，联合验证成功、失败、begin 故障零调用及 complete 故障无后续请求；不加载完整应用扫描或后续 CLI/ReAct/RuntimeConfiguration，不依赖 T040/T049/T052；schema 元数据使用执行入口可观测的测试替身，不依赖后续 T051。provider 模块保留并运行 T017/T018 选择与协议测试，不新增 provider→storage 横向依赖；证明默认与单次调用均禁用自动工具执行、schema callback 未执行、双目标只请求选中者，明确真实 DeepSeek 尚待 T065。

**Checkpoint**: provider 选择/协议测试及 boot 最小 Provider＋真实临时 SQLite 审计集成通过，可供 US1/US3 使用；不等待后续 CLI/ReAct 装配，真实工具闭环仍需最后联合验收，US2 不提前标为全部验收完成。

## Phase 4: User Story 1 - 初始化并与 Agent 多轮对话 (Priority: P1)

**Goal**: 最小工作区、配置快照、正文现读、内存会话、CLI 与无工具对话链可用。
**Independent Test**: 临时工作区初始化两次、三轮连续对话、两个会话隔离、21→20 历史投影、配置生效及正常退出；用 T026 已验证 Provider 或测试端口替身，不等待完整工具/REST/长期记忆体系。

### Tests for User Story 1

- [X] T027 [P] [US1] 在 `oryxos-cli/src/test/java/com/oryxlabs/oryxos/cli/WorkspaceInitializerTest.java` 先定义六目录/空白 Bootstrap、二次初始化文件字节不变、不可写/类型冲突不伪报成功、不自动创建默认 Agent 的隔离用例，对应 FR-001。
- [X] T028 [P] [US1] 在 `oryxos-core/src/test/java/com/oryxlabs/oryxos/core/agent/AgentContextContractTest.java` 先覆盖 frontmatter 派生与非法配置、目录穿越/越界软链接、正文/已引用 Bootstrap 每轮现读、启动快照不热更、Skill 只注入元数据且引用真实目标受控，对应 FR-002/004。
- [X] T029 [P] [US1] 在 `oryxos-memory/src/test/java/com/oryxlabs/oryxos/memory/SessionHistoryProjectionTest.java` 先覆盖结构化身份不碰撞、三轮/双会话、完整轮定义、21→20 可配置投影、当前工具链/系统指令不被裁剪、失败轮原始保留且不发送未配对消息；无 sessions 落库，对应 FR-003/004、SC-002。

### Implementation for User Story 1

- [X] T030 [US1] 在 `oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/WorkspaceInitializer.java`、`oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/InitCommand.java` 实现既有 oryxos.home/default .oryxos 解析与幂等六目录、空白 AGENTS.md/SOUL.md/USER.md 创建；不覆盖已有内容、不起 Spring、不造默认人格或 Agent，执行 T027。
- [X] T031 [US1] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/agent/AgentLoader.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/agent/ProfileRegistry.java` 从 AGENT.md frontmatter 派生并校验 T003 快照，工具/Provider/Bootstrap 引用有效才允许所选 Agent 使用；运行配置及引用列表固定，拒绝本功能尚不能兑现的 MCP/调度/长期记忆能力引用，不生成独立 Profile YAML。
- [X] T032 [US1] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/agent/ContextLoader.java` 每次模型调用前按契约顺序现读正文和已引用 Bootstrap，附当前日期时间；验证真实路径及 Skill 相对软链接目标，仅提供绑定 Skill 的 name/description/读取路径，不预载正文、不注册成工具，执行 T028。
- [X] T033 [US1] 在 `oryxos-memory/src/main/java/com/oryxlabs/oryxos/memory/InMemorySessionManager.java` 实现 SessionKey→当前会话、UUID 实例身份、串行输入、有序完整原始历史和失败轮状态；重启创建新 Session，不恢复旧对话，不为审计补建 sessions 表。
- [X] T034 [US1] 在 `oryxos-memory/src/main/java/com/oryxlabs/oryxos/memory/DefaultMemoryService.java` 实现 T005 门面，委托会话管理并返回最近 N 个完整轮+当前链的只读投影，保持原始历史不删、失败轮不作为完整历史送模型；不实现/注册假长期记忆能力，执行 T029。
- [X] T035 [US1] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/PromptBuilder.java` 组合单一系统上下文、MemoryService 历史投影、当前链和可用工具定义；不再直接拼接 Session.messages 造成重复，工具消息配对完整；后续 T049 复用同一构建路径。
- [X] T036 [US1] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ReActLoop.java` 建立通过 T023 AuditedProviderService 的基本无工具回答路径、非空回复校验与计次；工具执行扩展由 T049 完成，在其完成前遇工具请求不能静默成功或让 Spring AI 接管；所有模型调用必须经过审计。
- [X] T037 [US1] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/agent/AgentService.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/agent/ProfileContext.java` 实现统一 process(Session,String)、每次输入的 runId/当前轮与完成/失败状态、同会话串行及 finally 清理；ReAct 只调 MemoryService，不按 CLI/其他触发来源分支，不将全循环包进数据库事务。
- [X] T038 [US1] 在 `oryxos-channel-cli/src/main/java/com/oryxlabs/oryxos/channel/cli/CliChannel.java` 实现 stdin 顺序输入、stdout 回复、stderr 安全错误及 /quit/EOF；空白输入零调用，所有实际消息交给 AgentService，不新增历史面板、工具计数或调试标签。
- [X] T039 [US1] 在 `oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/ChatCommand.java`、`oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/OryxOsCommand.java`、`oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/OryxOsCliLauncher.java` 接入必需 --profile 与 RuntimeStarter/Handle，保持既有版本/帮助/无参行为；退出码沿用 0/1/2，独立 exec 缺完整 Runtime 时明确失败，精确错误文案须先审阅，不输出命令敏感参数或密钥。
- [X] T040 [US1] 在 `oryxos-boot/src/main/java/com/oryxlabs/oryxos/BootRuntimeStarter.java`、`oryxos-boot/src/main/java/com/oryxlabs/oryxos/RuntimeConfiguration.java`、`oryxos-boot/src/main/java/com/oryxlabs/oryxos/OryxOsApplication.java` 装配 T019/T020/T023/MemoryService/AgentService/CliChannel/AuditStore，Boot→CLI 注入 core 端口；chat 用 NON_WEB 且关闭时释放连接，init/help/version 不启动 Spring，Boot 原无参启动保持原行为；工具池默认空，不反向依赖 boot 或启动本功能外端口。
- [X] T041 [US1] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/CliRuntimeIntegrationTest.java` 验证主 JAR 路由及轻量入口不连库/模型、三轮与隔离、配置重启/正文现读、退出零新调用、关闭后审计仍可读且新进程不恢复旧历史；执行 T027～T029，并检查已有 CLI/Boot 测试保留；完整工具场景待 US3。

**Checkpoint**: 初始化和无工具多轮 CLI 闭环可隔离验证；这不是完整 Runtime MVP 或工具体系完成声明，真实工具联合验收仍待 T065。

## Phase 5: User Story 3 - 完成受控工具闭环 (Priority: P1)

**Goal**: 统一工具执行、顺序回填、请求去重、拒绝审计与循环停止，测试工具只在隔离上下文启用。
**Independent Test**: 可控模型依次返回工具请求、后续独立请求和最终回复；执行次数计数覆盖重复分发/同参数新请求，拒绝零实际操作，上限后零新调用。

### Tests for User Story 3

- [X] T042 [P] [US3] 在 `oryxos-core/src/test/java/com/oryxlabs/oryxos/core/runtime/ToolExecutorContractTest.java` 先写单/连续请求、重复分发、新 llmCallId 同参数可执行、认领中/未知不可重放、普通失败回填与审计致命错误不继续的用例，固定真实执行计数，对应 FR-008～011/015。
- [X] T043 [P] [US3] 在 `oryxos-tool/src/test/java/com/oryxlabs/oryxos/tool/WhitelistSandboxTest.java` 先写检查先于 IO、路径穿越/软链接越界、Shell 可执行文件与 argv、HTTP host 边界及拒绝执行次数零的 fixture；不调用真实 shell/远程服务，不增加测试专属 SandboxAction。
- [X] T044 [P] [US3] 在 `oryxos-provider/src/test/java/com/oryxlabs/oryxos/provider/ToolSchemaAdapterTest.java` 定义 schema 元数据转换和 callback 执行入口不可达测试；核对正确 ToolCallbacks 包名、assistant/tool ID 配对和一次响应重复 ID（含同内容）为协议错误、整批零工具执行，不只断言 JSON 能解析。

### Implementation for User Story 3

- [X] T045 [US3] 在 `oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/ToolRegistry.java`、`oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/DefaultToolInvoker.java`、`oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/ToolInputValidator.java` 实现 core 端口的注册、名称唯一性、Agent 可用子集、schema/工具契约参数校验和单次执行，不以“合法 JSON”替代参数校验；未知/未开放/非法参数拒绝，Registry/Invoker 不自做模型循环或再次审计。
- [X] T046 [US3] 在 `oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/Sandbox.java`、`oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/SandboxAction.java`、`oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/WhitelistSandbox.java` 实现中立 enforce 接口与实际 IO 前检查，覆盖 realpath/最近存在父路径、可执行文件+参数数组直传、HTTP host；禁止 bash -c 拼接，不实现/宣传强隔离，不增加产品文件/Shell/HTTP 工具，执行 T043。
- [X] T047 [US3] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ToolRequestIdentity.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ExecutionReceipt.java`、`oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ExecutionReceiptStore.java` 落实 data-model §4 原文“去重键为 `(runId, llmCallId, toolCallId)`；模型响应内的 toolCallId 必须非空且可唯一配对”，“RECEIVED → CLAIMED → COMPLETED；认领后不再重复执行；完成结果仅在内存收据中缓存”，“CLAIMED 尚未完成或结果不确定时禁止重新认领”；后续新 llmCallId 不误去重，不按参数哈希或跨重启重放。
- [X] T048 [US3] 在 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ToolExecutor.java` 按认领→begin 提交→校验/Sandbox/执行→complete 提交实现调度；拒绝记录 REJECTED/executed=false，执行失败记录 FAILED/executed=true，安全错误结果回填；完成失败传播审计致命错误、不重放，重复已完成请求只复用结果且不重复追加消息，执行 T042。
- [X] T049 [US3] 扩展 `oryxos-core/src/main/java/com/oryxlabs/oryxos/core/runtime/ReActLoop.java` 为工具请求先追加 assistant 原消息、顺序调用 ToolExecutor、按 ID 回填、再调用模型；先整批验证 ID 唯一；最大迭代默认 10、一次模型调用计一次，最后允许轮仍请求工具时终止不执行；失败轮保留原始历史但不伪造最终答案。
- [X] T050 [US3] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/fixtures/AcceptanceToolConfiguration.java`、`oryxos-boot/src/test/java/com/oryxlabs/oryxos/fixtures/PureCalculationTool.java`、`oryxos-boot/src/test/java/com/oryxlabs/oryxos/fixtures/DeniedActionTool.java` 仅测试源码注册无外部副作用计算/受控拒绝工具，建立执行计数和正确 ID 结果；不拷贝进 src/main，不新增默认测试 Agent、天气工具、昵称或备注。
- [X] T051 [US3] 在 `oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/ToolSchemaAdapter.java` 将 OryxTool 的 name/description/inputSchema 适配为 Spring AI 元数据，必要时用 @Tool 提取 schema；向 Provider 提供执行入口抛错的包装，真正执行只由 ToolExecutor，执行 T044，不注册 AGENT.md/Skill 成 Tool。
- [X] T052 [US3] 在 `oryxos-boot/src/main/java/com/oryxlabs/oryxos/RuntimeConfiguration.java` 完成 Registry/Invoker/ToolExecutor/Provider schema 的构造器注入及选中 Agent 的工具校验；默认业务池不注册测试工具，测试上下文显式注入 T050，模块无横向循环依赖，不改变已确认配置生效规则。
- [X] T053 [US3] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/ToolLoopIntegrationTest.java` 联合验证 T042/T044 的请求配对、顺序双工具、重复回调与新请求差异、最后迭代不执行及当前链完整；必须观察模型调用数和真实工具计数，不能凭回复文案判断一次执行，运行相关 core/tool/provider 测试。
- [X] T054 [US3] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditFailureStopsLoopTest.java` 注入模型/工具 begin 失败、工具动作后 complete 失败与结果未知，核对零后续模型/工具调用、零自动重放且不伪报回滚；普通工具失败仅在终态提交后可回填，审计失败不能作为普通错误再请求模型解释。
- [X] T055 [US3] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/DeepSeekAcceptanceTest.java` 编写显式 -Doryxos.acceptance.deepseek=true 启用的真实验收测试，使用 DEEPSEEK_API_KEY/DEEPSEEK_MODEL 和 T050；未开启明确跳过，开启缺参数失败，必须真实产生工具请求/一次执行/回填/最终回复与审计，不人工补造请求；本任务只准备入口，实际真实执行与证据在 T065。

**Checkpoint**: 可控工具闭环和失败停止通过；测试工具仍不进入正常用户环境，真实 Provider 不因测试类存在而视为已通过。

## Phase 6: User Story 4 - 追溯模型和工具调用 (Priority: P1)

**Goal**: 补齐全链路审计验收，不是此时才开始写审计。基础写入已在 Phase 2 与每次 Provider/Tool 入口前置。
**Independent Test**: 直接检查临时 SQLite，覆盖五类终态、前/后提交故障及提交结果未知，无需查询 API 或管理台。

### Tests and Completion for User Story 4

- [X] T056 [P] [US4] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditCoverageIntegrationTest.java` 覆盖模型成功/失败、工具成功/失败、Sandbox 拒绝五类实际调用或拒绝，逐项关联 session/run/iteration/llmCallId/toolCallId，验证覆盖率 100%、executed 与状态语义正确，对应 FR-012/013、SC-007。
- [X] T057 [P] [US4] 在 `oryxos-storage/src/test/java/com/oryxlabs/oryxos/storage/AuditCommitUncertaintyTest.java` 验证 begin 未确认提交零动作、complete 提交前失败保留 PENDING、提交结果未知可能已有终态且只读核实、不倒退终态；原文约束“PENDING 仅表示最终结果未持久确认，不意味着操作没发生”，不得在重启自动改 FAILED 或重放，关联 T054/SC-008。
- [X] T058 [P] [US4] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/SensitiveDataBoundaryTest.java` 将已知测试凭证嵌入配置异常、供应商原始错误、工具参数/结果和 cause，捕获终端/日志并读审计，确认零凭证、完整 Prompt、敏感参数或未经脱敏响应泄露；测试数据可丢弃，不使用真实 key，对应 FR-014、SC-006/007。
- [X] T059 [US4] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditRetentionIntegrationTest.java` 关闭并重建测试 Runtime 后读取此前提交的审计，核对可读且旧终态未丢、新 Session UUID 不与旧身份混淆、旧会话历史不恢复；不通过新增 sessions 表或审计响应正文缓存实现恢复。
- [X] T060 [US4] 在 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/RuntimeAssemblyBoundaryTest.java` 验证普通上下文默认工具池无 T050、测试配置不被组件扫描、chat 无 Web 监听、ProfileContext 在正常与异常 finally 均清理，审计只注入 core 端口；不把 Spring AI 的被动传递类依赖误当成可以启用响应式链或自动工具执行。
- [X] T061 [US4] 在 `oryxos-storage/src/test/java/com/oryxlabs/oryxos/storage/AuditFieldConstraintTest.java` 核对 T011/T012 的非空、枚举、唯一键、nullable token/boolean 与终态时间约束及数据映射；用缺失用量、零用量、同一请求重复和新独立请求证明实际语义，不编造 token、不默认保存 input_json/result_json。
- [X] T062 [US4] 运行 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditCoverageIntegrationTest.java`、`oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditFailureStopsLoopTest.java`、`oryxos-boot/src/test/java/com/oryxlabs/oryxos/AuditRetentionIntegrationTest.java` 及 storage 审计测试，逐项对照 `specs/002-provider-react-loop/quickstart.md` 审计场景保存安全验证结论；原始 Pending 无可靠证据就保持不确定，不新增审计修复命令或查询端点。

**Checkpoint**: 可控全链路的审计、安全与失败停止证据齐全；真实 DeepSeek 仍要完成 T065，不能以 US4 测试通过替代它。

## Phase 7: Polish & Cross-Cutting Concerns

本阶段只做已确认范围的收口，不进行无关重构或新增产品能力。

- [X] T063 按 `specs/002-provider-react-loop/quickstart.md` 在仓库根执行相关模块测试及 mvn clean package，保留 `oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar` 与 `oryxos-boot/target/oryxos.jar` 的产物证据；CLI 版本/帮助回归、Boot 最小 init/chat 和原无参行为分别核对，不能运行错误产物后误报能力完成。
- [X] T064 按 `specs/002-provider-react-loop/quickstart.md` 的隔离 CLI 场景验证幂等初始化、配置重启/正文现读、三轮对话、无默认测试工具、无未授权用户文案，复核 `oryxos-boot/target/oryxos.jar` 不含测试 fixture；已有用户文件与本次非目标改动保留，不部署或连接生产。
- [ ] T065 在具备操作者安全注入凭证/模型的隔离环境中实际执行 `oryxos-boot/src/test/java/com/oryxlabs/oryxos/DeepSeekAcceptanceTest.java`，使用 quickstart.md 的显式启用命令并核对 `oryxos-boot/target/surefire-reports/` 中该类实际执行且未跳过；真实多轮对话、模型工具请求、一次执行、回填、最终回复及持久审计全通过才勾选，凭证缺失/模型未请求工具/零测试/跳过均不得算完成，不能以假模型替代。
- [X] T066 仅依据已实际完成的 T016/T026/T041/T053/T062/T063～T065 证据更新 `docs/IMPLEMENTATION_STATUS.md`，区分本功能设计、实现、可控测试、真实 Provider 验收及未覆盖的长期记忆/MCP/三个最终 Demo；若真实验收未通过须如实保留部分状态，不把整个 Runtime MVP 标为已完成；向用户交付变更文件、测试命令结果及保留原改动，不自动提交、推送或部署。

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1（T001–T002）
  → Phase 2（T003–T016：公共契约 + 审计可用）
  → US2（T017–T026：Provider 可控验证）
  → US1（T027–T041：CLI/内存对话）
  → US3（T042–T055：工具闭环 + 真实验收入口）
  → US4（T056–T062：全路径审计验收）
  → 收口（T063–T066：构建、隔离 CLI、真实 DeepSeek、状态记录）
```

四个故事都是 P1；编号不改变 Provider 先行和审计前置原则。US2 的真实完整验收与 US1/US3 共享 T065，
它依赖完整 Runtime，故不是 T026 的前置条件，也不能在 US2 检查点先宣称已通过。
US4 的完整证据后置不等于审计实现后置；T016 未通过不得放行实际模型/工具调用。

### Within Phase 2

T003/T004 可并行；T005–T008 在这两项完成后可分文件并行；T009 在相应错误/审计契约就绪后完成。
T010 先建立行为契约；T011 与 T012 在 T007/T010 和固定数据字段契约就绪后并行；
T013 依赖 T011/T012，T014 依赖存储基础，T015 验证初始化，T016 为强制门槛。
不同文件是必要条件，但任何端口尚未完成时不得提前执行依赖它的实现。

### Within Stories

- US2：T017/T018 在 Phase 2 后可并行；T019→T020，T021 在 T018 契约后，T022 依赖前述适配，T023 接审计；T024 修改 T020 同文件，必须顺序；T025→T026 收口；T026 位于 boot 测试源码，仅依赖 Phase 2 审计基础与 T019～T025 的最小接线，不依赖后续 US1/US3 完整装配，避免与 T040/T052 形成任务循环或新增 provider→storage 依赖。
- US1：T027/T028/T029 在 US2 后可并行；T030–T032 分别实现初始化/配置/正文；T033→T034→T035→T036→T037→T038→T039→T040→T041。T040 的默认空工具装配不能注册业务测试工具，US3 统一执行链在 T052 接入。
- US3：T042/T043/T044 在 US1 后可并行；T045/T046 提供工具检查与 Sandbox；T047→T048→T049，T050 仅测试，T051/T052 做 schema 与 Boot 接线，T053/T054 验证后 T055 准备真实验收入口。
- US4：T056/T057/T058 在 US3 后可并行；T059/T060/T061 补齐关闭/隔离/字段行为后 T062 统一核对。
- T063/T064 不替代 T065；T066 依据真实结果记录，不得为交付方便把失败/跳过写成完成。

## Parallel Opportunities

只允许在上述前置门槛完成后运行以下互不重叠的文件组；不建议并行修改 POM、RuntimeConfiguration 或 ReActLoop。

| 范围 | 可并行任务 | 独立文件/条件 |
|---|---|---|
| 公共模型 | T003、T004 | Profile/Agent 与 Session/Turn 文件分离 |
| 公共端口 | T005、T006、T007、T008 | T003/T004 完成后分 provider/memory、tool、audit、runtime 文件 |
| 存储映射 | T011、T012 | 固定字段契约后分别写 schema 和实体，不并行变更字段含义 |
| US2 | T017、T018 | 选择测试与协议测试各自 fixture，不共享可写临时文件 |
| US1 | T027、T028、T029 | CLI 初始化、core 上下文、memory 历史三组测试 |
| US3 | T042、T043、T044 | core 执行、tool 安全、provider schema 测试 |
| US4 | T056、T057、T058 | Boot 覆盖、storage 提交不确定、Boot 脱敏测试，独立临时库 |

[P] 合计 19 项。这里的并行是工程实施安排，不新增 OryxOS 多 Agent 编排功能。

## Requirements Coverage

| 规格 | 主要实现/验证任务 |
|---|---|
| FR-001 | T027、T030、T039～T041、T064 |
| FR-002 | T003、T028、T031、T032 |
| FR-003/004 | T004、T029、T033～T037、T041、T059 |
| FR-005 | T017～T024、T026（boot 最小集成）、T055、T065 |
| FR-006/007 | T009、T017～T019、T025、T026（boot 最小集成）、T058 |
| FR-008/009 | T006、T042、T044、T047～T049、T051～T055 |
| FR-010/011 | T003、T042～T049、T053、T054 |
| FR-012/013 | T007、T010～T016、T023、T048、T056、T059、T061、T062 |
| FR-014 | T009、T019、T025、T058 |
| FR-015 | T010、T013、T023、T048、T054、T057、T062 |
| FR-016 | T002、T050、T052、T055、T060、T064 |
| SC-001/002 | T027～T041、T059、T064 |
| SC-003/006 | T017～T026（T026 为 boot 最小集成）、T055、T058、T065 |
| SC-004/005 | T042～T054、T065 |
| SC-007/008 | T010～T016、T023、T048、T054、T056～T062、T065 |

以上是任务覆盖映射，不是已经执行的测试覆盖率，也不是 speckit-analyze 报告。

## Implementation Strategy

最小技术增量为 Phase 1/2 审计基础 + US2 Provider 可控选择 + US1 无工具 CLI 多轮对话；不能删掉审计来缩成 MVP。
本功能最终交付仍必须含 US3、US4 及真实 DeepSeek 联合验收，不把增量可运行误当本功能全部完成。
先写每个故事的行为测试并确认能暴露缺失行为，再完成对应实现；正常测试不联网、不启用真实 Provider。
后续用户若明确授权 Implement，也须先审阅待新增的精确用户可见错误文案；原有授权不包含额外界面或默认业务内容。
Tasks 阶段后已经过只读 Analyze 和用户授权的 I1/U1 修订；本次依据用户单独调用 Implement 执行，剩余真实验收保持未完成。


## Implement 验证记录（2026-10-29）

- 已完成 T001–T064、T066，共 65 项；T065 未执行，未以默认跳过替代真实验收。
- 用户已审阅并确认 SafeErrorMapper 的 9 条固定错误文案；只附安全字段路径与 UUID 调用编号。
- `mvn -o -q clean package` 成功：55 项测试，54 通过、1 跳过、0 失败、0 错误；随后 Provider 同类型双目标专项回归通过。
- 最终 Boot/CLI JAR 的轻量入口、隔离三轮对话、配置重启/正文现读、审计持久性、主产物不含测试 fixture 及 Boot 原无参路径验证通过。
- begin/complete 提交故障与结果不确定均停止后续动作；已提交终态不倒退，PENDING 不自动重放或臆断为未执行。
- T065 需要安全注入 DEEPSEEK_API_KEY 与 DEEPSEEK_MODEL 后显式启用验收；当前两者均缺失，未请求真实模型。
- 实施状态和产物指纹见 `docs/IMPLEMENTATION_STATUS.md`。本功能不等于完整 Runtime MVP，Kimi、长期记忆、MCP、九工具和三个最终 Demo 仍未验收。
- 保留原有作业/学习记录改动；本次不创建分支、不提交、不推送、不部署。
