# CLI、工作区与配置契约

日期：2026-10-29。以下为待实现契约，不表示当前已有这些业务命令。
关联：[规格](../spec.md)、[研究 R6/R10](../research.md)、[Runtime 契约](runtime.md)。

## 1. 最小入口和运行产物

主要 Runtime 产物：`oryxos-boot/target/oryxos.jar`；Boot 将 init/chat 路由到 Picocli，
以 core RuntimeStarter 端口向 CLI 注入 Boot 装配，不使 CLI 模块依赖 Boot。
`java -jar oryxos.jar init` 初始化；`java -jar oryxos.jar chat --profile <agentName>` 对话。
工作区用既有 JVM 系统属性 `-Doryxos.home=/absolute/path` 指定，默认当前目录 `.oryxos`。
本功能不新增 status、profile CRUD、serve、gateway、会话查询或审计修复命令。

已有 CLI exec JAR 的 --version/--help/无参提示保留；init 可纯文件操作。
独立 exec 不含完整装配时 chat 返回运行配置失败，不能悄悄启动另一份不完整 Runtime。
Boot 的 init/帮助/版本入口不创建 Spring 上下文或连接数据库/模型；chat 使用 NON_WEB 上下文，不启动监听端口。
Boot 原无参启动保持原行为，不将其描述为本功能完成 REST 或服务模式。

## 2. 初始化

创建 agents、skills、output、memory、sessions、logs 六个子目录及空白 AGENTS.md、SOUL.md、USER.md。
空白引导文件不自动填入人格或额外产品文案。存在的目录和文件不覆盖，不自动造默认 Agent。
已有用户文件保留字节内容。路径不可写/类型冲突清晰失败，不宣称完整初始化成功；不删已有内容来重试。
数据库不依赖 init 启动 JPA：首次 chat 在任一模型或工具调用前完成显式 schema 初始化和检查。
sessions 目录只是兼容已裁决工作区布局，不表示本阶段保存对话文件或支持恢复。

## 3. Agent 定义与引导文件

操作者在 `agents/<agentName>/AGENT.md` 提供 frontmatter 与正文。
必需 name、provider.name、provider.model；tools 默认为空，bootstrap 按显式引用读取；
max_iterations 默认 10，max_history_turns 默认 20，均为正整数。
未实现的 MCP、调度或长期记忆功能不能被配置成已可用；在已选 Agent 中明确拒绝无法兑现的能力引用。
Profile 启动时快照，正文和已引用 Bootstrap 每次模型调用前重读。变更 bootstrap 引用集合同样需重启。
上下文顺序采用已配置 identity.prompt、Agent 正文、按配置顺序的 Bootstrap、当前日期时间；
近期会话与当前轮由 MemoryService 提供一次。不要把技能正文预载进系统上下文。
上下文文件缺失或越界先报安全定位错误，不把错误路径/文件内容整段放进 Prompt。

## 4. Provider 配置与凭证

运行配置使用 Spring 的外部配置入口，例如 JVM 属性 `spring.config.additional-location` 指向操作者的 runtime.yaml。
配置约定为 `oryxos.providers.<providerName>.base-url`、`api-key`；API key 的值必须为环境变量占位符。
可选 connect-timeout/read-timeout 为正时长，默认 10s/60s；启动时快照，不能配置无限等待。
AGENT.md 中 provider.name 引用显式 providerName，provider.model 可引用外部模型名环境变量。
环境变量替换只解析允许的配置字符串，未定义时明确失败，不能保留字面占位符继续请求。
Provider 和模型名均不可从 API key、Bean 类型或默认厂商猜测。错误只显示安全字段位置，不打印解析后的凭证。

## 5. 交互与退出

从 stdin 顺序读取文本，stdout 用于回复，stderr 用于脱敏错误和必要日志；不打印完整 Prompt 或调试调用体。
支持已有技术方案中的 `/quit` 以及 EOF 退出；空白输入不调用模型。
不新增历史查看、工具计数面板、调试标签或未经确认的提示文案。精确错误文案在实现前审阅。
选定 Agent 不存在或无效、上下文无效、运行装配缺失时启动失败；缺失密钥不触发模型请求。
普通模型/工具失败结束当前输入或按 Runtime 的失败回填规则处理；审计故障停止该次处理后不自动重放。
若下一次用户输入发生在审计仍不可用时，begin 仍拒绝调用；不把手工再次输入偷偷当原操作重试。

## 6. 可观察的退出结果

沿用 Picocli 0=正常、1=运行失败、2=参数用法错误；不新增外部错误码体系。
init 成功、正常退出为 0；启动配置/装配/审计失败为 1；缺失必需 --profile 等用法错误为 2。
交互中的失败不打印“成功”假象；自动化验证通过内部结果和审计检查，不依赖未确认的精确文案。

## 7. 验收测试隔离

默认工具集为空，允许普通多轮对话；本功能不向默认用户注册测试计算工具。
工具闭环在 Boot 专属验收测试上下文启用纯计算工具；CLI stdin/stdout 集成可用可控模型运行，
真实模型与工具闭环由同一 Runtime 端口的隔离测试验证。测试 fixture 不打进主产物。
