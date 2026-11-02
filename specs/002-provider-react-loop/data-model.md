# 数据模型：Provider 抽象与 ReAct 循环

日期：2026-10-29。本文是设计，尚无实体、建表脚本或业务数据实现。
来源：[规格](spec.md)、[研究](research.md)、[Runtime 契约](contracts/runtime.md)。

## 1. AgentDefinition / ProfileSnapshot

| 属性 | 约束与含义 |
|---|---|
| name | 必填，等于 agents 下目录名；禁止路径分隔符、`.`、`..`；重复名拒绝 |
| providerName / model | 必填非空；providerName 必须存在于显式配置映射中；不推断厂商 |
| temperature | 可选，有限数值；供应商范围按所选适配器验证，不统一假设所有模型支持 |
| tools | 工具名集合，不能重复；只引用已注册并对当前 Agent 开放的工具 |
| bootstrap | 工作区内文件引用列表，启动时校验存在与真实路径边界，顺序保留 |
| maxIterations | 正整数，默认 10；一次模型调用计一次迭代 |
| maxHistoryTurns | 正整数，默认 20；只限制模型输入中的已完成历史轮数 |
| identity.prompt | 已有 frontmatter 的可选指令；不生成新 persona 或昵称 |
| body / bootstrap content | 正文及引导内容不进入配置快照，每次模型调用前重新读取 |

Profile 从 AGENT.md frontmatter 派生，只有进程内对象，不落独立 YAML 或 Profile 数据库表。
工作区物理路径、软链接目标每次使用前验证；日志只报告安全路径/字段信息，不打印配置值。
配置快照启动后固定，正文重读不能顺带使 Provider、工具列表或限制热更新。
Skill 绑定遵守宪章的相对软链接边界，只提供元数据，不在本功能引入完整 Skill 执行工具。

## 2. ProviderDefinition

- providerName 为唯一非空键，映射到显式创建的 ChatModel；相同类型模型可以有多个具名实例。
- model 非空且由 Agent 选择；API key 为外部环境变量引用，解析后的值仅用于内存客户端装配。
- baseUrl 必须是无 userinfo、无凭证 query 的 HTTP(S) URL；真实验收使用官方 HTTPS，测试使用回环地址。
- 出错时返回字段路径和受控错误类别；不回显凭证、原始响应体或 Authorization。
- 可配置多个模型服务，但本功能真实验收只要求 DeepSeek；两个可控目标验证路由独立性。

## 3. Session / Turn / Message（只在内存）

| 对象 | 属性与规则 |
|---|---|
| SessionKey | channel、userId、agentName 的结构化三元组，禁止用不带边界的字符串拼接造成碰撞 |
| Session | sessionId=随机 UUID，会话实例唯一；同一进程用 SessionKey 找当前会话；重启不恢复 |
| Run / Turn | runId=UUID；对应一次用户输入；状态 RUNNING、SUCCEEDED、FAILED、LIMIT_REACHED；带创建/结束时间 |
| Message | messageId、turnId、顺序号、role、内容；role 为 USER、ASSISTANT、TOOL；系统上下文另行构建 |
| Assistant tool request | 保留模型 toolCallId、工具名及参数；与产生它的 llmCallId 关联 |
| Tool result | 以相同 toolCallId 回填，包含成功/失败和受控错误，不伪造成功 |

一个完整交互轮从用户输入开始，到最终回复结束，含期间全部工具链。
原始历史保留失败和达到上限的轮，不伪造最终模型回复；失败/中断轮不作为完整历史轮送入下一轮，
下一轮仍只取最近 N 个完整轮与新的当前轮，避免发送未配齐的工具消息。
历史裁剪是模型输入的投影，不修改原始消息集合；系统指令和当前处理链不参与 N 轮淘汰。
同一会话处理串行化，避免两个输入交错破坏消息次序；不同会话隔离，无多 Agent 编排。

## 4. ToolRequestIdentity / ExecutionReceipt

- 去重键为 `(runId, llmCallId, toolCallId)`；模型响应内的 toolCallId 必须非空且可唯一配对。
- 单次模型响应的工具请求 ID 必须唯一；同 ID 不论内容是否相同都属于协议错误，整批实际工具执行数为零，不猜测对应关系。该情况不同于合法解析请求的重复分发。
- RECEIVED → CLAIMED → COMPLETED；认领后不再重复执行；完成结果仅在内存收据中缓存。
- 重复分发已完成请求复用同一结果，不新增一次执行、不重复追加同一条工具结果。
- CLAIMED 尚未完成或结果不确定时禁止重新认领；审计完成失败视为致命处理失败。
- 后续不同 llmCallId 即使参数、名称或供应商 toolCallId 相同，仍是独立请求。
- 不按参数哈希去重，不实现跨重启 exactly-once，不自动重放；retryable 只是失败信息。

## 5. SQLite 审计：共同字段

数据库位置固定为 `${oryxos.home}/oryxos.db`，沿用当前应用配置。
本功能只建两张审计表，不建 sessions 表；session_id 为关联快照，不设到 sessions 的外键。
表设计字段如下，具体 DDL 留到实现，必须是显式幂等脚本，保持 ddl-auto: none。

| 字段 | 逻辑类型及约束 |
|---|---|
| id | TEXT UUID，主键，调用前生成 |
| session_id / profile_name / run_id | 非空关联快照，不保存用户输入内容 |
| iteration | INTEGER，1 至当前快照的 maxIterations |
| status | PENDING、SUCCEEDED、FAILED；工具另有 REJECTED |
| success | nullable BOOLEAN；PENDING 为 NULL，成功 true，已确认失败/拒绝 false |
| started_at | 非空 UTC 时间 |
| completed_at / duration_ms | PENDING 可空；终态必填；耗时用单调时钟计算且非负 |
| error_code / error_message | 成功为空；失败为受控错误码和脱敏摘要，不直接使用原始异常消息 |

### llm_calls 专属字段

provider、model 必填；prompt_tokens、completion_tokens、total_tokens 为 nullable 非负整数。
用量缺失写 NULL，不能写零冒充；不从估算补造官方值，不保存完整 Prompt 或响应正文。
每一次底层实际 HTTP 尝试必须对应一次调用；适配器隐藏重试关闭，见 Runtime 契约。

### tool_invocations 专属字段

llm_call_id、tool_call_id、tool_name 必填，executed 为 nullable BOOLEAN：
PENDING=NULL（不能由此断言未执行），SUCCEEDED=true；校验拒绝 REJECTED=false；
执行后失败 FAILED=true。参数校验失败归 REJECTED，不存在的工具名仍可审计。
唯一键 `(run_id, llm_call_id, tool_call_id)`，不保存原始 input_json/result_json。
llm_call_id 是关联值，可用索引查询，实际外键约束不要求扩大为会话持久化。

## 6. 审计状态转换与事务

1. 独立短事务提交 PENDING 后，才允许发起模型请求或进入工具检查/执行链。
2. IO 不在数据库事务中；工具拒绝同样完成 REJECTED 审计，executed=false。
3. 独立短事务以 PENDING 为前置条件写终态并提交；提交成功才可正常返回结果或继续调用。
4. begin 失败：调用次数为零。complete 在提交前失败则保留 PENDING；提交结果不确定时可能已有终态，停止后续调用并只读核实，不重放、不伪报回滚，不把已提交终态改回 PENDING。
5. PENDING 仅表示最终结果未持久确认，不意味着操作没发生。崩溃后也不能自动改为 FAILED 或重试。
6. 同一记录不覆盖已确认终态；审计写入异常作为致命失败，不转换成可供模型继续处理的普通工具错误。
7. 修复后的补记限核实原记录并补充可证明的事实，不触发原操作。无可靠证据保持不确定；不新增修复命令或 UI。

## 7. 生命周期与最小持久化范围

Agent 配置/Bootstrap 存文件；Profile、Session、消息和工具收据仅内存；审计存 SQLite。
不引入长期记忆后端、通知渠道、调度表或 Session 自动恢复；审计记录不承担对话历史备份。
正常关闭释放 Spring 上下文和数据库连接；关闭不删除已提交审计。
