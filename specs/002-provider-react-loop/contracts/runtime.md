# Runtime 行为与内部端口契约

日期：2026-10-29。设计约定，尚未实现；供后续任务拆解和测试，不是新增 REST API。
关联：[规格](../spec.md)、[数据模型](../data-model.md)、[计划](../plan.md)。

## 1. 依赖倒置与职责

| 契约/实现 | 归属和边界 |
|---|---|
| AgentService、ReActLoop、PromptBuilder、AgentLoader、ContextLoader | core；统一处理 Session 和用户输入，不识别触发来源 |
| ProviderService 端口、供应商无关消息/响应 | core；实际 Spring AI 适配在 provider |
| MemoryService、SessionManager、LongTermMemoryStore 端口 | core；最小会话门面和内存会话实现在 memory；本功能不实现长期记忆后端 |
| OryxTool、ToolCatalog/ToolInvoker、ToolResult 端口 | core；注册表和实际适配在 tool |
| ToolExecutor | core；负责请求去重、执行调度、审计和回填结果；不另建自动工具引擎 |
| Sandbox、WhitelistSandbox | tool；统一 enforce(SandboxAction)，实际 IO 前校验 |
| AuditStore 端口及安全审计 DTO | core；JPA 与显式建表在 storage |
| RuntimeStarter/RuntimeHandle 端口 | core；Boot 创建并向 CLI 注入实现，避免 cli → boot 依赖 |

`AgentService.process(Session, String)` 只管理一次输入的完整处理，finally 清理 ProfileContext。
会话历史写入由 MemoryService 门面委托 SessionManager；不让 ReAct 直接操作特定记忆后端。
未装配长期记忆是本功能范围，不注册空壳能力并宣传为已完成 Memory。

## 2. 一次处理

1. 空白输入不调用模型；校验选定 Profile 和安全上下文，获取该会话的串行处理权。
2. 创建 runId/当前轮，用户输入只追加一次；配置使用启动快照，正文/已引用 Bootstrap 每次构建现读。
3. Prompt = 一个系统上下文 + 最近 N 个完整历史轮 + 当前用户输入及当前完整工具链 + 工具定义。
   MemoryService 返回历史一次，PromptBuilder 不再第二次拼接 Session.messages。
4. 迭代从 1 开始，每次 Provider 调用消耗一次；达到 maxIterations 后不得发起下一次模型或工具调用。
5. Provider 返回无工具且文本非空：保留回复、结束该轮；空文本且无工具属于协议错误。
6. 有工具请求则先追加原始 assistant 请求消息，按顺序调用 ToolExecutor，回填匹配的结果，再进入下一模型迭代。
7. 最后一个允许的模型迭代若仍请求工具，直接终止为 LIMIT_REACHED，不执行无法再向模型回填处理的工具；原始链留在失败轮，不作为完整历史重放。
8. 普通工具失败在审计成功提交后可作为失败结果回填；审计故障立即结束本次处理，不再调用模型解释错误。
9. 用户退出/EOF 不生成新调用。异常失败不能被包装为成功回复，未来恢复不能悄悄重放旧操作。

## 3. Provider 契约

- 通过显式 providerName → ChatModel 映射选择模型实例；model 来自选定 Profile，未选实例不接收请求。
- 每次底层调用关闭自动工具执行，保留模型生成的 ToolCall ID、名称、参数和原 assistant 消息顺序。
- 工具回填使用 Spring AI 的 assistant/tool response 消息结构，禁止将工具结果混为普通 user 文本。
- schema 来自 OryxTool；@Tool/ToolCallback 仅用于生成定义，不交给框架执行。
- 失败、超时、鉴权拒绝转成受控错误类别，未知用量保留 NULL；不记录响应原文和请求原文。
- 隐藏的 Provider HTTP 重试关闭；不做 Fallback、自动路由、流式。每一次真实调用必须与审计一一对应。
- 单次请求设置有限超时：连接默认 10 秒、读取默认 60 秒，均可在 Provider 配置中覆盖为正时长，随运行配置重启生效。JDK HttpClient.connectTimeout 配合 JdkClientHttpRequestFactory.setReadTimeout(Duration)，通过 RestClient 请求工厂注入；不将该参数宣传为整次任务延迟 SLA。
- DeepSeek 的实际依赖接口及当前协议兼容处理以 research.md 的版本证据为准。

## 4. ToolExecutor、检查与重复请求

- 先验证单次模型响应的工具 ID 全部唯一，重复 ID 无论参数是否相同均作为协议错误，整批不执行工具；对合法请求再按身份认领、提交 begin 审计，随后检查 Agent 可用工具、参数与 Sandbox，再执行。
- ToolCatalog/ToolInvoker 不执行审计或再次调用模型，实际执行只由 ToolExecutor 调度一次。
- 工具实现拥有实际动作的 SandboxAction 信息；无 IO 的纯计算不伪装成文件或网络访问。
- 拒绝路径用测试专用带动作声明的桩工具验证：校验先发生，计数器执行次数保持零；无需触碰危险资源。
- WhitelistSandbox 的中立接口不新增“测试专用动作类型”；路径需比较 realpath/最近已存在父路径，拒绝穿越与越界软链接。
- 同一次请求重复回调复用进程内结果，不重复执行或追加消息；认领中/结果未知状态不自动重试。
- 同名同参数但来自下一次独立模型响应的请求允许执行，不以参数哈希去重。
- 本阶段工具执行尝试默认一次；失败的 retryable 仅供模型判断后续独立动作，不隐式重放。

## 5. 安全与失败分类

CONFIGURATION、CONTEXT_READ、PROVIDER、PROTOCOL、TOOL_VALIDATION、TOOL_EXECUTION、
SANDBOX_DENIED、ITERATION_LIMIT、AUDIT_UNAVAILABLE 是内部受控类别，不要求新增产品标签。
错误呈现的精确中文文案由实现前审阅，不在 Plan 中新增对正常用户的调试面板或审计提示。
未经脱敏的异常 cause 不直接进入终端、日志或审计；不以正则替换一次作为唯一脱敏保证。
工具的输入/输出正文不默认落库；审计只接收允许字段与受控摘要，不让任意工具对象直接序列化。
上下文读取/初始化属于应用自己的文件边界校验；不把应用自身建库读配置错误算成一次模型工具调用。

## 6. 审计故障与恢复边界

AuditStore 必须提供 begin（提交登记）和 complete（提交终态）两个同步阶段，并传播提交失败。
用独立短事务/TransactionTemplate，完成 flush 后确认 commit 返回；不能只调用 save 就宣称持久化。
不在外层 AgentService 上开启覆盖远程调用的大事务，不用异步队列降级审计。
数据库忙、锁、磁盘只读等错误均遵循停止规则。操作后 complete 在提交前失败时留下 PENDING；提交结果未知时可能已有终态，停止并只读核实，不覆盖已有终态，不能再次执行原调用。
保存成功结果的工具内存收据不能掩盖其审计未完成；任何再次分发都返回该次处理致命失败而不是重新执行。
进程异常退出后的 PENDING 不自动重放；本功能只提供人工核实边界，不新增审计修复工具。

## 7. 测试工具及验收隔离

测试工具定义仅在各模块测试源码或 Boot 的隔离验收测试上下文注册；默认业务工具池不新增测试工具。
实际 DeepSeek 验收通过测试上下文注入纯计算工具，仍经过真实 Provider、ReAct、ToolExecutor 和 SQLite 审计。
不新增测试 profile 作为正常 CLI 的默认入口，不为演示编造天气、昵称或运营备注。
可控 ChatModel/HTTP 测试端点覆盖协议与两个目标路由；拒绝和重复分发场景通过观察执行次数验证。
这些证据不替代后续九个工具、MCP 或三个最终 Demo。
