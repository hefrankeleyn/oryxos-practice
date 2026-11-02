# Implementation Plan: Provider 抽象与 ReAct 循环

**Branch**: `main`（实际 Git 分支） | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: `specs/002-provider-react-loop/spec.md`，含 2026-10-29 确认的 5 项澄清。
**Status**: Plan Phase 0/1 设计已生成，待审阅；未生成 Tasks，未编写、构建或验收业务实现。
setup-plan 返回的 BRANCH=002-provider-react-loop 是其功能上下文标识，不是实际 Git 分支；本轮没有切分支。

## Summary

在现有 9 模块骨架中设计最小初始化、Agent 配置加载、Provider 显式选择、进程内多轮会话及自主管理的 ReAct。
Provider 基于锁定版 Spring AI 同步协议适配，工具只通过 ToolExecutor 调度，真实接入验收限 DeepSeek。
会话历史在内存保留；发送模型时投影最近默认 20 个完整轮及当前完整链；正文/Bootstrap 每轮现读。
两张 SQLite 审计表在每次调用前登记、调用后完成，失败停止，不等到 Session 持久化或 REST 才建设。
工具闭环通过隔离测试工具验收，默认用户工具池不增加产品工具。

研究结论见 [research.md](research.md)，数据约束见 [data-model.md](data-model.md)，
接口见 [CLI](contracts/cli.md) 和 [Runtime](contracts/runtime.md)，未来验证见 [quickstart.md](quickstart.md)。

## Technical Context

**Language/Version**: JDK 21；同步阻塞，必要并发使用 virtual thread。

**Primary Dependencies**: Boot 3.5.9；Spring AI 1.0.3；保留 Alibaba BOM 1.0.0.4；Picocli 4.7.7；
现有 SnakeYAML/Jackson、Spring Data JPA、SQLite JDBC、Hibernate community dialect、SLF4J。
新增普通 spring-ai-openai 依赖由根 BOM 管理，不引入 Agent starter 或升级版本。

**Storage**: Agent/Bootstrap 为文件；Session/Turn/消息/执行收据为内存；审计为 `${oryxos.home}/oryxos.db`。
显式建表脚本，ddl-auto: none；只做本功能两张审计表，无 sessions、调度、通知或长期记忆表。

**Testing**: 既有 JUnit 5、AssertJ、Boot 测试；可控 ChatModel、JDK 回环 HTTP fixture、临时目录和临时 SQLite。
真实 DeepSeek 通过显式启用的隔离验收测试，不在普通测试时自动联网。

**Target Platform**: 本地 macOS 开发、JDK 21 的可信 Linux 单机；无容器强隔离承诺。

**Project Type**: Maven 多模块单体 Runtime，最小 CLI 接入；没有本功能业务 REST 或管理台。

**Performance Goals**: 本功能不新设延迟/吞吐承诺；以迭代次数受限、单次 HTTP 尝试、历史投影正确为可验证边界。
最终核心阶段性能和三个 Demo 要求保留，本轮不声称满足全量压测目标。

**Constraints**: core 不依赖内部功能模块；Provider 显式映射；关闭框架自动工具执行和隐藏重试；
审计故障停止，不能无审计继续；无完整 Prompt/凭证日志；测试工具不默认进入正常用户环境。

**Scale/Scope**: 每次 CLI 进程选择一个 Agent，会话按身份隔离；会话输入串行；不引入多 Agent 协作、
MCP、完整九工具、长期记忆实现、自动路由、流式或定时任务。

## Constitution Check

GATE：研究前和 Phase 1 设计后各检查一次。两次设计检查均通过；“通过”不表示实现/测试通过。

| 宪章约束 | 研究前 | 设计后证据 |
|---|---|---|
| I：JDK 21、9 模块、同步及依赖倒置 | PASS | 模块树和 core 端口；无新增模块/业务响应式链 |
| II：自实现循环、显式映射、禁自动工具执行 | PASS | research R1–R5、Runtime §2–3；不交框架调度 |
| III：Agent 派生 Profile、Memory 门面、工具归属 | PASS | data-model §1–3；工具实现/Sandbox 归 tool |
| IV：凭证外部注入、真实 IO 前校验、安全日志 | PASS | CLI §3–5、Runtime §4–5；拒绝测试零实际 IO |
| V：LLM/Tool/拒绝持久审计、显式建表 | PASS | 两阶段短事务；begin 未确认提交零调用，complete 失败停止并核实可能已提交的结果 |
| 已确认阶段范围 | PASS | 不保存会话，不默认注册测试工具，不新增 REST/调度 |
| 文件与交付边界 | PASS | 只改本功能设计产物，不改宪章/规格/业务源码/旧 Hello/作业 |

说明：Spring AI 适配器的传递流式类依赖不代表采用响应式运行栈，业务只允许同步 call；
现有 POM、模块或文档中后期能力不能转化为本功能的额外需求。

## Project Structure

### Documentation (this feature)

```text
specs/002-provider-react-loop/
├── spec.md                         # 保持已澄清原文
├── checklists/requirements.md       # 已有质量清单，保持不变
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── cli.md
    └── runtime.md
```

本轮不生成 tasks.md；以上新文件均为设计资料。

### Source Code (repository root)

以下是未来实现的职责路径，不代表这些类型已经存在：

```text
oryxos-core/src/main/java/com/oryxlabs/oryxos/core/
  model/       # Profile、Session、Turn、Message、请求/结果
  agent/       # AgentLoader、ContextLoader、AgentService、ProfileContext
  runtime/     # ReActLoop、PromptBuilder、ToolExecutor、RuntimeStarter/Handle
  provider/    # ProviderService 端口与供应商无关 DTO
  memory/      # MemoryService、SessionManager、LongTermMemoryStore 端口
  tool/        # OryxTool、ToolCatalog、ToolInvoker、ToolResult 端口
  audit/       # AuditStore、调用生命周期与脱敏 DTO
oryxos-provider/src/main/java/com/oryxlabs/oryxos/provider/
  # Spring AI 适配、显式模型映射、DeepSeek 窄协议适配
oryxos-memory/src/main/java/com/oryxlabs/oryxos/memory/
  # DefaultMemoryService、InMemorySessionManager；无长期后端实现
oryxos-tool/src/main/java/com/oryxlabs/oryxos/tool/
  # ToolRegistry、ToolInvoker 实现、Sandbox、WhitelistSandbox
oryxos-storage/src/main/java/com/oryxlabs/oryxos/storage/
  # 两表 JPA 实体/审计实现
oryxos-storage/src/main/resources/db/schema/
  # 显式幂等审计建表脚本，非 Flyway migrations
oryxos-channel-cli/src/main/java/com/oryxlabs/oryxos/channel/cli/
  # CliChannel：只负责输入/输出和统一 AgentService 调用
oryxos-cli/src/main/java/com/oryxlabs/oryxos/cli/
  # 保留原入口，增加 InitCommand、ChatCommand、ConfigLoader 和启动端口接线
oryxos-boot/src/main/java/com/oryxlabs/oryxos/
  # 聚合装配、NON_WEB chat 启动、Boot → CLI 的端口注入
oryxos-web/
  # 保留骨架，本功能不增加业务接口
oryxos-*/src/test/
  # 有效测试归各模块；隔离工具仅测试源码，不进主产物
```

**Structure Decision**：所有核心契约和供应商无关 DTO 在 core，功能模块只依赖 core；Boot 聚合注入。
Provider/Tool 不直接依赖 storage，ReAct 不直接访问 JPA 或 SessionManager；CLI 不反向依赖 Boot。
Runtime 启动端口不是新增产品能力，用于解决现有 CLI 与 Boot 的装配边界。

## Phase 0 — 研究已收敛

- 准确版接口：本地 spring-ai-model、Spring Web、Spring Retry 二进制 + Spring AI v1.0.3 官方源码。
- DeepSeek：非推理工具调用，用环境模型名和窄请求适配；兼容性须先做协议 fixture 后做真实验收。
- CLI/Boot：主要 Runtime 使用现有 Boot JAR，轻量 CLI exec 保留；通过 core 启动端口避免循环依赖。
- 数据：审计表不外键关联不存在的 sessions 表；begin/complete 短事务与实际 IO 分离。
- 去重：runId + llmCallId + toolCallId，不按参数哈希；不做隐式重放。
- 历史与上下文：最近完整轮投影，不删除原始历史；失败轮保留但不发送未配对消息。

剩余的是实现验证，不存在需要替用户选择的未裁决需求或悬空技术选型。

## Phase 1 — 数据流与契约

Boot 分派 → CLI/Channel → AgentService → MemoryService/ContextLoader → PromptBuilder →
ProviderService → 显式 ChatModel；有工具时由 ToolExecutor → ToolInvoker/Registry → Sandbox → 工具执行，
结果关联后继续。每次 Provider/Tool 入口都先 begin 审计，返回/继续前 complete。
同一会话串行，ProfileContext 在 finally 中清理，审计提交失败不转成普通可继续工具结果；结果未知只读核实，不倒退已提交终态。

工具 schema 和工具执行分离。无 IO 的测试计算工具不伪装成真实产品工具；Sandbox 拒绝通过隔离测试桩验证。
本轮没有类体、迁移代码、完整测试或实现任务清单。

## Validation Strategy

| 规格要求 | 后续验证证据 |
|---|---|
| FR-001/002、SC-001 | 幂等初始化、已有文件哈希不变、Agent/frontmatter/路径校验 |
| FR-003/004、SC-002 | 三轮与两会话隔离；21→20 投影；当前链完整；配置重启/正文现读 |
| FR-005/006/007、SC-003/006 | 双可控目标；缺失密钥/非法配置零请求；真实 DeepSeek 对话及工具链 |
| FR-008/009、SC-004 | 单工具与连续两工具；重复分发一次执行；新模型轮同参数仍可执行 |
| FR-010/011、SC-005 | 未知/未开放/非法参数/失败/拒绝；上限后零新调用；不走自动工具执行 |
| FR-012/013/014、SC-007 | 五类终态审计、脱敏、缺失用量 NULL、退出后数据可读 |
| FR-015、SC-008 | begin/complete 故障注入；提交前故障 PENDING 不伪报，提交结果未知只读核实；后续零调用、零重放 |
| FR-016 | 测试工具只进测试上下文，正常主产物默认工具池不增加 |

命令和预期结果详见 quickstart.md；本轮仅静态设计核验，不运行 Maven、真实模型或产品 Demo。
新依赖缺失、DeepSeek 参数窄适配、打包启动接线都必须在实现后独立验证，不能凭设计声称通过。

## Complexity Tracking

没有宪章违规或需要例外豁免的设计。
先登记后完成审计、启动端口及 DeepSeek 请求适配是本功能必要的内部设计，不增加产品功能。
