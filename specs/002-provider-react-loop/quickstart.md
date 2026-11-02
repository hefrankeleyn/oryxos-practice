# 后续验证指南：Provider 抽象与 ReAct 循环

日期：2026-10-29。**以下是实现后的目标验证步骤，当前骨架尚不能执行 init/chat。**
本次 Plan 没有运行这些步骤；未提供完整实现代码、SQL 或测试套件。
先阅读 [CLI 契约](contracts/cli.md)、[Runtime 契约](contracts/runtime.md) 和 [数据模型](data-model.md)。

## 1. 前置条件

- JDK 21、Maven、挂载的工作卷和可写临时目录；根目录依赖版本保持不变。
- 后续实现已完成相应类、显式 schema、启动接线和测试；不能拿当前骨架执行目标命令并当作验收。
- 默认所有测试使用隔离临时工作区、SQLite 和可控模型，不接生产系统。
- 真实验收时，操作者安全注入 DEEPSEEK_API_KEY 与 DEEPSEEK_MODEL；不要把密钥值写入文档、命令历史或提交。
- DEEPSEEK_MODEL 选择当时可用、支持非推理工具调用的实际模型；不假设旧模型别名永久有效。

## 2. 构建与自动化验证（实现后）

在仓库根目录执行：

```bash
mvn -pl oryxos-core,oryxos-provider,oryxos-memory,oryxos-tool,oryxos-storage,oryxos-channel-cli,oryxos-cli,oryxos-boot -am test
mvn clean package
```

要求：相关单元/协议/集成测试全部成功，打包成功；普通测试不调用真实 Provider。
真实验收未启用时必须明确记录“未执行”，不能把跳过计为真实通过。
确认仍生成 Boot oryxos.jar 和 CLI exec JAR，版本/帮助原有测试保留。

## 3. 工作区与最小 CLI（实现后）

以下 shell 变量只用于操作者隔离验证，不修改现有工作区：

```bash
repo_root='/Volumes/Work-Data/专业/11-AI/06-AI工程化实战/02-projects/oryxos-practice'
run_home="$(mktemp -d)"
java -Doryxos.home="$run_home" -jar "$repo_root/oryxos-boot/target/oryxos.jar" init
```

记录生成的必要目录与空白 Bootstrap；自行在一个文件加入 fixture 内容并记录哈希，再执行一次 init，
要求原字节不变。模拟无权限/路径类型冲突时明确失败，不清理已有文件强行通过。
不自动生成默认 Agent、不填入人格或额外产品文案。

操作者在临时工作区提供 runtime.yaml，只有凭证引用：

```yaml
oryxos:
  providers:
    deepseek:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
```

在 `agents/fixture-agent/AGENT.md` 提供仅用于该隔离验收的定义，例如：

```yaml
---
name: fixture-agent
provider:
  name: deepseek
  model: ${DEEPSEEK_MODEL}
tools: []
bootstrap: [AGENTS.md, SOUL.md, USER.md]
settings:
  max_iterations: 10
  max_history_turns: 20
---
```

正文由操作者提供，此片段不增加自动生成的用户可见说明。默认工具池为空，可验证普通对话。
启动目标命令：

```bash
java -Doryxos.home="$run_home" -Dspring.config.additional-location="file:$run_home/runtime.yaml" -jar "$repo_root/oryxos-boot/target/oryxos.jar" chat --profile fixture-agent
```

发送三条有关联的消息，核对上下文；修改正文/已引用 Bootstrap 后下一模型轮生效；
改 Provider/模型/工具/限制，不重启仍按旧配置，重启才加载新配置。用 /quit 或 EOF 退出。
独立 CLI exec JAR 只验证轻量版本/帮助/初始化，不把缺失 Runtime 的 chat 失败当完整能力已实现。

## 4. 需要自动化观察的场景

| 场景 | 必须观察的结果 |
|---|---|
| 两个会话 | 不混入对方消息；重启创建新会话，不要求恢复旧历史 |
| 21 个完整轮后继续 | 模型输入只有最近 20 个完整历史轮 + 系统指令 + 当前完整链；原始历史不删除 |
| 调整历史上限 | 重启后按新上限投影，不拆工具请求/结果 |
| 双可控 ChatModel/回环端点 | 只请求显式指定目标；名称/参数不能被静默猜测 |
| 密钥缺失/配置非法 | 实际模型请求计数为 0，错误可定位且凭证不泄露 |
| 当前 DeepSeek 协议 | 请求中 thinking.disabled、路径和模型正确；messages/tools/tool ID 不变；HTTP 实际尝试一次；重复工具 ID 的非法响应不执行工具 |
| 延迟响应与超时 | 配置有限连接/读取超时生效；超时反馈可定位、审计为失败，无隐藏重试 |
| 单次/连续工具请求 | assistant 请求先于 tool 结果，结果 ID 正确，最终回复可关联 |
| 重复分发/独立新请求 | 前者只执行一次；后者即使参数相同也执行，不按参数哈希误去重 |
| 未知/未开放/非法参数/失败/拒绝 | 明确失败反馈、正确审计；拒绝后实际执行计数为 0 |
| 最大迭代次数 | 达上限不再发起下一模型/工具调用；最后一轮请求工具不能越界执行 |
| 审计 begin 故障 | 新调用计数为 0 |
| 审计 complete 提交前故障 | 原操作不重放、后续调用计数为 0、PENDING 保留，不伪报回滚 |
| 审计提交结果不确定 | 已提交终态不被改回 PENDING；停止并只读核实，无重放、不伪报完整审计 |
| 进程退出及脱敏 | 已提交审计仍可读；无 key/完整 Prompt/敏感参数/原始模型响应入日志或审计 |

上述场景用临时数据及可控计数器验证，不靠截取自然语言回复推断内部执行次数。
不向正常用户界面展示调试计数、内部审计状态或测试工具。

## 5. 真实 DeepSeek 工具闭环（实现后，显式启用）

后续 Boot 测试源码提供 `DeepSeekAcceptanceTest`，仅测试上下文注册纯计算工具；不写入默认 Agent 配置或主产物。
该类使用真实 Provider、实际 ReAct/ToolExecutor 及临时 SQLite，包含多轮对话和工具结果回填。
未开启时明确跳过；显式开启后，密钥/模型缺失应失败而非以假响应替代。

```bash
mvn -pl oryxos-boot -am -Dtest=DeepSeekAcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false -Doryxos.acceptance.deepseek=true test
```

命令中的测试类/开关属于本设计的未来测试入口，目前不存在。
检查报告中该验收类实际执行且未跳过；必须有真实工具请求、一次执行、结果回填、最终回复以及对应审计。
模型未请求预期工具、供应商不支持所选模型或协议不兼容，都算未通过；不能人工补造工具请求或以可控模型替代。
Kimi 不在本功能真实验收范围；仍需保留最终核心阶段双 Provider 和三个 Demo 的后续要求。

## 6. 审计核对与结束

退出 chat 后直接读取临时 SQLite，检查五类记录及故障 PENDING；无需实现审计查询 API。
PENDING 不是“操作没执行”，不自动改成 FAILED，不重新调用模型/工具来补数据。
本指南不提供修改审计数据的通用命令；补记必须核实原记录并遵循 Runtime 契约。

记录实现状态、测试命令/结果、真实 Provider 是否执行、完整 Demo 未覆盖项。
只清理本次创建的临时 fixture，保留验收必要的脱敏证据；不删除既有用户工作区或生产数据。
这些步骤不包含提交、推送、部署或网站发布。
