# 研究与设计决策：Provider 抽象与 ReAct 循环

日期：2026-10-29。阶段：Plan Phase 0。以下是设计决定和接口证据，不是代码实现或真实服务验收。
依据：[已澄清规格](spec.md)、仓库 AGENTS.md、constitution 1.0.0 和现有 POM/源码。

## R1. 保留版本，按准确接口设计

**Decision**：保持 JDK 21、Boot 3.5.9、Spring AI 1.0.3、Spring AI Alibaba BOM 1.0.0.4、Picocli 4.7.7。
DeepSeek 首个适配采用普通 `org.springframework.ai:spring-ai-openai` 模块，版本来自根 Spring AI BOM；
不引入自动配置 starter，不升级 BOM，不为首个 Provider 引入 Alibaba 的 Agent 抽象。

**Rationale**：当前 provider 仅依赖 spring-ai-model，占位工程没有现成的网络模型客户端。
复用 OpenAI 兼容协议模块符合 Provider 复用边界，可显式创建多个同类型模型。
**Alternatives considered**：升级新版 Spring AI、依赖全套 starter、原生 DeepSeek 适配；前两者扩大兼容范围，后者同样缺少当前扩展参数入口，未采用。

**证据**：本机 settings.xml 指定的 Maven 仓库为 `/Volumes/Work-Data/data/maven-repository`，不能误把旧 ~/.m2 缓存当当前环境。
已通过 JDK 21 javap 核对缓存中的 spring-ai-model 1.0.3、spring-web 6.2.15、spring-retry 2.0.12。
本地没有 spring-ai-openai 1.0.3 JAR；该适配器 Builder 由准确 tag 源码核对，尚未下载、编译或联调。

## R2. Spring AI 只转换协议和 schema，Runtime 自己循环

**Decision**：`ChatModel.call(Prompt)` 单次调用；默认和每次请求的 `internalToolExecutionEnabled(false)` 都显式设置，
OpenAiChatModel 的工具执行资格谓词固定 false。Provider 只提供 schema 元数据包装，意外调用 callback 执行入口直接失败。
`ToolCallbacks.from(Object...)` 可用于 @Tool schema 生成，正确包名为 `org.springframework.ai.support.ToolCallbacks`。

**Rationale**：本地 ChatResponse.hasToolCalls、AssistantMessage.getToolCalls 与 ToolResponseMessage 对应 ID 可用，
无需把 ToolExecutor 交给框架。ToolCallback 的执行入口与 JSON Schema 定义应分离。
**Alternatives considered**：使用 ChatClient 自动工具循环或 DefaultToolCallingManager 执行；违背宪章，拒绝。

官方准确版本源码：
- [OpenAiChatModel v1.0.3](https://raw.githubusercontent.com/spring-projects/spring-ai/v1.0.3/models/spring-ai-openai/src/main/java/org/springframework/ai/openai/OpenAiChatModel.java)
- [OpenAiChatOptions v1.0.3](https://raw.githubusercontent.com/spring-projects/spring-ai/v1.0.3/models/spring-ai-openai/src/main/java/org/springframework/ai/openai/OpenAiChatOptions.java)

## R3. DeepSeek 当前协议与旧 SDK 的窄适配

**Decision**：模型名从外部 `DEEPSEEK_MODEL` 提供，不硬编码旧示例别名。
采用官方 Chat Completions 的非推理工具调用模式，DeepSeek 专属 RestClient 请求拦截器仅在匹配的 POST 请求 JSON
补入 `thinking.type=disabled`，保留 Spring AI 生成的 model/messages/tools/tool_call_id 字段，更新 Content-Length。
URL 使用 `https://api.deepseek.com` 加 `/chat/completions`，显式设置 completionsPath，避免重复拼接 /v1。
拦截器只执行一次请求；不打印 body/header，不自行重写完整 Provider 协议或调用工具。

**Rationale**：[DeepSeek 官方工具文档](https://api-docs.deepseek.com/guides/tool_calls/)当前非推理示例显式关闭 thinking，
并要求原 assistant 工具请求与对应 tool 结果回填；同版 OpenAiChatOptions 和 DeepSeekChatOptions 未提供该扩展字段。
[OpenAiApi v1.0.3](https://raw.githubusercontent.com/spring-projects/spring-ai/v1.0.3/models/spring-ai-openai/src/main/java/org/springframework/ai/openai/api/OpenAiApi.java)
提供 baseUrl、apiKey、completionsPath、restClientBuilder；本地 Spring Web 二进制确认支持 requestInterceptor 和替换请求字节。

**Alternatives considered**：依赖旧模型别名、开启 thinking 后忽略 reasoning 字段、升级 SDK、绕过 Spring AI 全手写 HTTP；均不采用。
**验证边界**：未来必须用本地 HTTP 协议 fixture 验证注入字段、路径、消息配对、执行次数和隐藏重试，再做真实模型验收。
官方模型名和可用性会变化，运行时按操作者选定的当前模型验证；当前文档检查不等于服务真实通过。
该决策只采用非推理模式，不扩展到 Responses API、推理链持久化、严格 schema Beta 或其他服务。

## R4. 每个真实尝试一条审计，不让框架暗中重试

**Decision**：给 OpenAiChatModel 提供 maxAttempts=1、noBackoff 的 RetryTemplate；传输层不得自动重发请求。
本阶段工具默认单次尝试，不自动重放；retryable 保留为失败信息，后续模型主动产生的独立请求有新身份。
单次请求连接默认 10 秒、读取默认 60 秒，允许正时长配置并在重启后生效。JDK HttpClient 设置连接超时，
RestClient 使用 JdkClientHttpRequestFactory 并设置读取超时；其 Duration 入口已用本机 Spring Web 6.2.15 javap 直接核实。
迭代次数不能结束卡住的单次请求，故需延迟响应 fixture；这些是内部超时参数，不新增整体耗时 SLA。

**Rationale**：默认 SDK 重试可能把多次远程操作藏在一次审计中，也会影响去重与失败停止要求。
已在准确版本模型源码确认 retryTemplate 可注入，本地 Spring Retry 二进制确认相关 Builder 方法。
**Alternatives considered**：沿用框架默认重试或工具三次退避；本阶段不采用，防止重复操作。后续显式重试必须逐次审计且不复用同一请求执行身份。

## R5. 同步栈及传递依赖边界

**Decision**：业务只用阻塞 RestClient、Spring MVC 既有骨架及 virtual thread，不增加 WebFlux starter/server 或流式订阅。
[OpenAI 适配器 POM](https://raw.githubusercontent.com/spring-projects/spring-ai/v1.0.3/models/spring-ai-openai/pom.xml)
含流式 SDK 所需 spring-webflux 传递依赖；这是 SDK 类依赖，不是采用响应式业务执行模型，不直接强行排除造成类缺失。

**Rationale**：保留库加载需求与明确同步调用边界，同时遵守“不引入响应式栈”的运行架构约束。
**Alternatives considered**：引入 WebFlux 服务或调用 stream API；拒绝。实现时依赖检查和同步调用测试必须证明没有启用响应式运行路径。

## R6. Boot 聚合运行，CLI 不反向依赖 Boot

**Decision**：主要运行产物为现有 Boot `oryxos.jar`，支持最小 init/chat 分派。
core 定义 RuntimeStarter/RuntimeHandle，boot 注入实现给 cli；cli 普通 JAR 继续被 Boot 聚合，不依赖 boot 或功能模块。
init/help/version 不启动 Spring；chat 用 Boot 装配创建 NON_WEB 上下文，使用完关闭。
现有 CLI exec JAR 保留轻量版本/帮助/初始化；独立 exec 没有完整 Runtime 时不能冒充可运行 chat，需明确失败。
Boot 原无参启动保持现有行为，本功能不借此新增 serve/gateway 或业务 REST。

**Rationale**：复用现有双产物，解决 CLI 无法获得 Runtime 的实际依赖问题，保持 9 模块和单向依赖。
**Alternatives considered**：cli 依赖 boot 形成环、CLI 聚合所有功能形成横向依赖、直接改变全部打包契约；均不采用。
详细启动形式见 [CLI 契约](contracts/cli.md)，这些是本功能设计而不是已存在命令。

## R7. 会话只在内存，审计单独持久化

**Decision**：SessionManager 只管理进程内会话。数据库固定 `${oryxos.home}/oryxos.db`，仅本功能两张审计表。
审计 session_id 是实例 UUID，保留 profile/run 快照，不设置 sessions 外键，不保存对话正文。
SessionKey 保存 channel/user/agent 语义，每次进程新建会话有新的 sessionId，避免新旧审计混淆。

**Rationale**：满足已确认的阶段范围，不让审计实体关系反向迫使实现会话持久化。
**Alternatives considered**：提前建 sessions 表/消息恢复、所有审计只内存、SQLite MEMORY 后端；前者超范围，后两者不满足本功能或宪章。

## R8. begin/complete 两阶段审计与不确定结果

**Decision**：调用前独立事务提交 PENDING；事务外执行；调用后独立事务提交终态。
begin 未确认提交不调用；complete 提交前失败保留 PENDING，提交结果不确定时可能已有终态，停止并只读核实，不倒退终态、不重放、不伪报回滚或完整审计。
工具拒绝也在 begin 后走拒绝终态，executed=false；PENDING 的 executed 为 NULL，不能声称未执行。

**Rationale**：SQLite 本地事务不能与远程模型原子提交。外层大事务回滚会擦除已发生操作的证据。
**Alternatives considered**：先执行后首次落库、异步审计、事务包住整个循环、磁盘失败时退回日志；拒绝。
修复后只对可核实事实补原记录，不自动触发原操作，不新增恢复 UI/命令；无法核实的结果保持不确定。

## R9. 历史投影、配置快照与失败轮

**Decision**：启动派生 Profile、校验 Provider/工具/限制；每次调用前重读正文及启动时引用的 Bootstrap。
MemoryService 一次性提供最近 N 个完整轮 + 当前轮，PromptBuilder 不重复拼接 Session 历史。
失败/中断轮保留在进程内原始历史，不带未配对工具链进入下一完整对话；当前轮始终完整，不以 token 估算切断。

**Rationale**：兑现已确认 20 轮规则，明确“裁输入”与“删历史”的区别，避免文档中的重复历史注入。
**Alternatives considered**：删除原始历史、全部上下文缓存、动态更新运行配置、LLM 总结压缩；均不在本功能。
若当前完整链超过供应商上下文限制，按 Provider 失败反馈并审计，不悄悄截断或自动总结。

## R10. 隔离工具、配置来源与范围控制

**Decision**：真实验收测试在 Boot 测试上下文注册纯计算工具，不向生产 CLI 默认工具池注册测试工具。
init 只创建必要目录和空白 Bootstrap 文件，不自动生成带人格/名称/业务指令的新 Agent。
用户或测试 fixture 提供 Agent 定义；外部 runtime.yaml 经既有 Spring 配置入口加载，凭证只有环境占位符。

**Rationale**：满足工具闭环同时避免新增未经确认的产品工具、默认人格、演示数据或调试 UI。
**Alternatives considered**：新增天气 Tool、默认示例 Agent、测试工具进生产 profile、填充昵称；均拒绝。

## 研究结论与剩余验证

无剩余需求澄清项；打包、审计事务、会话关联、请求去重和配置加载已有具体设计。
本地已有通用接口的二进制证据，以及准确版本的适配器源码证据；新依赖解析/编译、协议 fixture、
真实 DeepSeek、Maven 打包和实际 CLI 验收均尚未执行，留给实现与验收阶段，不能标记通过。
