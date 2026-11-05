# OryxOS 实施状态

> 最后核对：2026-11-04（Asia/Shanghai）
>
> 核对范围：当前本地工作树。本文只记录实现与验证状态，不替代需求文档、技术方案或路线图。
>
> 2026-10-27 补充核对：Spec Kit Hello 页面、网站发布与作业目录；其他工程状态沿用 2026-10-03 的核对结果。

## 状态口径

| 状态 | 含义 |
|---|---|
| ✅ 已实现 | 已有实际代码，并完成与当前范围相称的本地验证 |
| 🟡 部分实现 | 只有骨架、局部能力或配置，尚未完成端到端验收 |
| ⬜ 未实现 | 只有目标设计、模块占位或文档描述，没有可用实现 |
| ⏸ 未启用 | 已预留接入点，但配置为空或明确暂不开启 |

“本地验证通过”不等于“已部署”，更不等于“线上用户验收通过”。部署、外部 Provider、GitHub/Giscus、真实设备或业务系统验收必须单独记录。

## 当前结论

OryxOS 已有项目文档、9 模块工程和双语网站，现已增加首个功能的 Provider 显式映射、自实现 ReAct、进程内会话、最小 init/chat 和 SQLite 调用审计。
本功能的可控测试及本地 JAR 验证通过；真实 DeepSeek 验收尚未执行，不能将本功能或整个 Runtime MVP 标为全部验收完成。

当前生产 Java 源文件共 78 个（排除 `package-info.java`），覆盖九模块；web 已有公共响应、异常处理、输入校验与请求日志，尚无业务 Controller。
JUnit 报告覆盖 29 个测试类、63 项测试：62 项通过、1 项真实 DeepSeek 验收默认跳过，0 失败、0 错误。
测试工具只在测试源码中，正常工具池为空；尚未实现完整九工具、MCP、长期记忆、会话恢复及最终三个 Demo。

003 基础设施的日志、隔离管理端口、MVC/API 公共规范、Google 格式及静态质量检查已完成本地验证。
用户已确认并采用阿里规约兼容映射与人工清单，不声称运行完整 P3C。
依赖漏洞扫描因缺少 NVD API key 和可用漏洞数据，按用户选择保留待配置；不能把全套安全检查标为通过。
使用指引见 [FOUNDATION.md](FOUNDATION.md)，完整证据见 [003 验收记录](../specs/003-project-foundation/quickstart.md)。

## 工程与交付物

| 项目 | 状态 | 当前证据 | 尚缺内容 |
|---|---|---|---|
| 六份项目资料 | ✅ 已实现 | `docs/01` 至 `docs/06` 已存在 | 仍需收口跨文档冲突 |
| 根目录 Agent 规则 | ✅ 已实现 | `AGENTS.md` 为共用规则，`CLAUDE.md` 为 Claude Code 入口 | 后续随架构决策维护 |
| 9 模块 Maven 骨架 | ✅ 已实现 | 根 POM 聚合 9 个模块，clean package 通过 | 完整 Runtime MVP 的其余业务能力 |
| Spring Boot 入口 | 🟡 部分实现 | Boot JAR 分派 init/chat；chat 为 NON_WEB；无参 MVC、虚拟线程及隔离 Actuator 已验证 | 完整服务命令、业务 REST 与产品健康接口 |
| CLI 入口 | 🟡 部分实现 | 独立 exec 保留版本/帮助/无参提示并支持 init；完整 Boot JAR 提供 chat | 真实 DeepSeek 验收及其余 CLI 命令 |
| 项目网站 | ✅ 已实现 | VitePress 中英文首页与 5 类文档页；本地构建通过 | 线上可达性和发布结果需独立验收 |
| GitHub Pages 工作流 | ✅ 已验证 | 2026-10-27 的 `Deploy website` 执行成功（提交 `83a5af0`），线上 `/hello` 实际访问通过 | 后续发布结果仍需逐次核对 |
| Giscus 评论 | 🟡 部分实现 | 仓库 ID 和 Announcements 分类 ID 已写入 `integrations.ts` | 部署后登录、创建 Discussion、回显评论的真实验收 |
| 51.la 统计 | ⏸ 未启用 | `id`、`ck` 保持为空，代码会自动关闭 | 用户决定是否开通并提供配置 |
| Apache 2.0 许可证文件 | ⬜ 未实现 | README 与 POM 已声明 Apache 2.0 | 根目录缺少 `LICENSE` 正文 |
| Java 持续集成 | 🟡 已配置 | 新增 Java quality 与 dependency-audit 作业；本地 verify 通过，原网站发布 workflow 保留 | 远端运行、NVD secret 与分支保护未验证 |
| 日志、监控与 API 基础设施 | ✅ 已实现 | JSON/可读日志、MDC、回环管理端口、统一 API 组件与本地文档；测试及真实 JAR 验证通过 | 业务 REST 端点及执行期限另行实现 |
| 格式与静态代码检查 | ✅ 已实现 | Google 格式、Checkstyle、PMD、SpotBugs/FindSecBugs、Gitleaks；负向样例有效，暂存 hook 已激活 | 自动检查不替代完整人工安全审查 |
| 阿里规约兼容映射 | ✅ 已接入选定范围 | 用户确认的 PMD 7/Checkstyle 部分映射、覆盖缺口和人工清单；九模块规则检查通过 | 不是完整 P3C；未自动覆盖的条款仍需按清单人工审查 |
| 依赖漏洞检查 | 🟡 待配置 | Dependency-Check profile 与 CI 已配置并保留失败行为 | 按用户选择暂缓，待 NVD key/漏洞库就绪后完成真实扫描 |
| Spec-Kit artifacts | ✅ 已建立 | 宪章与 001/002 产物保留；003 初始化规格、计划、任务及验证记录已建立 | 002 的 T065 及 003 的 T016 未完成 |
| 训练营作业文档 | ✅ 已纳入版本管理 | `docs/homework/` 保存第一、第二周作业、实操记录与截图；第二周第二题已提交上游 [PR #1050](https://github.com/oryx-labs/oryxos/pull/1050) | 等待上游批准工作流及审查合并 |

## Runtime MVP 核心能力

| 能力 | 状态 | 当前证据 | 完成判定 |
|---|---|---|---|
| US-1 Provider | 🟡 部分实现 | 具名 ChatModel 映射、同步 OpenAI 协议适配、DeepSeek 非推理参数、有限超时及 LLM 审计；双目标/协议测试通过 | 真实 DeepSeek 对话与工具联合验收；最终核心阶段 Kimi 验收 |
| US-2 ReAct | 🟡 部分实现 | ReActLoop、PromptBuilder、ToolExecutor、AgentService；顺序工具回填、去重和上限可控测试通过 | 真实 DeepSeek 工具闭环及最终核心 Demo |
| US-3 Memory | 🟡 部分实现 | MemoryService 门面与内存 SessionManager；完整轮投影、隔离及原始历史保留通过 | MarkdownMemoryStore、长期记忆功能与跨重启恢复 |
| US-4 Tool / MCP / Sandbox | 🟡 部分实现 | 统一注册/参数校验/执行适配、WhitelistSandbox、工具审计；隔离纯计算与拒绝测试通过 | 九个产品工具、MCP stdio 及完整 Demo；不承诺强隔离 |
| US-5 REST API | 🟡 基础设施已实现 | 统一响应/异常、32KiB UTF-8 校验组件、MDC 与受控 OpenAPI；尚无生产业务 Controller | 10 个核心端点、产品健康接口、同步执行期限及真实调用验收 |
| SQLite 业务持久化 | 🟡 部分实现 | llm_calls/tool_invocations 显式 DDL、JPA 与独立 begin/complete 提交；成功/失败/拒绝及提交不确定验证通过 | Session、调度、通知等其余业务表；不支持会话恢复 |
| CLI Channel | 🟡 部分实现 | stdin/stdout 多轮接入，统一 AgentService；Boot JAR 回环目标与持久审计验证通过 | 真实 Provider 联合验收；其他渠道不在本功能 |
| 12 个 CLI 命令 | 🟡 部分实现 | 最小 init/chat 已实现，旧版本/帮助入口保留；轻量 exec 的 chat 无 Runtime 时明确失败 | 其余命令及全量集成 |
| Agent 加载与上下文 | 🟡 部分实现 | AGENT.md frontmatter 派生、配置快照、正文/Bootstrap 现读、Skill 元数据与路径边界测试通过 | read_file 按需读 Skill 正文及完整 Memory/Tool 联动 |
| AgentScheduler | ⬜ 未实现 | 仅有文档设计 | cron 触发、防重入、执行记录和同链路调用测试通过 |
| 三个验收 Demo | ⬜ 未实现 | 仅有需求和技术方案描述 | 每日天气、每日科技日报、每日 GitHub 日报分别保留可重复运行证据 |

## 最近一次本地验证

### 2026-11-04：基础设施初始化

- 用户原有全部 Git 可见改动已保存为 main 上的 `d969be1`、`bfcca5f`；开始时即位于 main，无需额外合并。2026-11-05 按用户要求将基础设施初始化纳入后续 main 本地提交，保留 NVD 扫描待办；未推送或部署。
- `./mvnw -o -B -ntp clean package` 与最终 `./mvnw -o -B -ntp verify` 通过；63 项测试中 62 项通过、1 项真实 DeepSeek 默认跳过。
- 九模块 PMD 无违规/分析错误；SpotBugs/FindSecBugs 实际扫描 104 个生产 class，无未处理违规/分析错误。已审查例外逐项限定类、方法或字段并记录理由。
- 121 个 Java 文件重复格式化再校验，文件哈希保持不变；110 个原有 Java 文件的格式迁移另有隔离生成的审查 patch。当前源码敏感信息扫描及格式/PMD/FindSecBugs/Gitleaks 负向样例通过。
- 实际 Boot JAR 在临时 SQLite 与回环端口验证管理入口、探针、Prometheus、统一 404、JSON 日志和文档开关；Swagger WebJar 及编码路径绕过被阻断。进程与临时目录已清理。
- 两种 JAR 的版本/帮助/init，以及现有 chat/审计可控测试通过；项目测试类未进入发布 JAR。没有运行真实付费 Provider 或三个最终 Demo。
- `.githooks/pre-commit` 已在本仓库本地激活；暂存快照检查不会改写工作树或索引。Java CI 文件已加入，未运行远端 CI、修改分支保护或部署。
- 用户随后确认采用 PMD 7 + Checkstyle 兼容规则映射，已补齐 [覆盖说明与人工清单](../config/quality/alibaba-mapping.md)，并收紧包名小写与空 catch 检查；`./mvnw -o -B -ntp compile pmd:check` 九模块通过。本轮未改变 Java 业务源码，原构建/JAR 验收证据继续适用。
- 联网依赖扫描实际失败于 NVD 空凭证和无漏洞数据，用户选择保留待配置；没有报告可证明依赖安全。完整命令、范围、报告与产物哈希见 [003 验收记录](../specs/003-project-foundation/quickstart.md)。

### 2026-10-29：首个 Provider/ReAct 功能

- `mvn -o -q clean package` 及后续 `mvn -o -q package` 回归：成功，55 项测试中 54 项通过、1 项真实 DeepSeek 默认跳过，0 失败、0 错误。
- `mvn -o -q -pl oryxos-provider -am test`：Provider 专项回归通过，包含同类型双目标、工具消息配对、单次 HTTP 尝试、超时、未知用量及凭证回显拒绝。
- 最终 Boot JAR 在临时目录、两个回环 HTTP fixture 上验证：幂等 init、三轮对话、运行配置重启生效、正文/Bootstrap 现读、会话重启不恢复、已提交审计仍可读、默认无测试工具。
- Boot 与独立 CLI JAR 的版本/帮助/init/必需参数及拼错命令返回 2、零建库验证通过；Boot 原无参启动在临时目录及回环随机端口验证通过，随后停止测试进程。
- 审计集成覆盖 LLM 成功/失败、Tool 成功/失败、Sandbox 拒绝、提交前失败和提交后确认丢失；敏感参数、原始异常及完整 Prompt 不进入日志/审计，已知凭证回显被拒绝。
- 主 Boot JAR 和嵌套内部模块 JAR 均未包含验收 fixture 或集成测试类。
- Boot JAR SHA-256：`0004bc403a0aad05822ed0dfb8c209479c9c976466556b758a22d1d1e594a2df`。
- CLI exec JAR SHA-256：`b0c08bb3c0bf663120087530c678e861b8d6b5394d9e19e537dee1d44d38ff88`。
- **真实 DeepSeek 未执行**：当前进程环境缺少 `DEEPSEEK_API_KEY` 和 `DEEPSEEK_MODEL`，T065 保持未勾选；默认跳过不算验收通过。真实验收命令见 `specs/002-provider-react-loop/quickstart.md`。
- 本轮未提交、推送、部署或执行最终三个 Demo；原有作业文档和学习记录改动保留。

### 2026-10-03：历史骨架验证

2026-10-03 已执行：

```text
mvn test
  BUILD SUCCESS
  4 tests, 0 failures, 0 errors, 0 skipped

mvn package
  BUILD SUCCESS

java -jar oryxos-cli/target/oryxos-cli-0.1.0-SNAPSHOT-exec.jar --version
  成功输出 OryxOS 0.1.0-SNAPSHOT、构建时间、Java 与操作系统信息

npm run docs:build
  VitePress build complete
```

这些验证只能证明工程骨架、CLI 版本入口、Spring 上下文与网站构建正常，不能证明 Runtime MVP 已实现。

### 2026-10-27：Hello 页面合入与作业目录整理

- 将 `homework/spec-kit-hello-world` 的两个提交快进合入 `main` 并推送，保留原提交历史。
- `npm run docs:build` 通过，产物包含 `website/.vitepress/dist/hello.html`。
- 本地浏览器确认 `/hello` 可直接访问、刷新，固定文案正确，返回链接在当前标签到达原中文首页。
- [Deploy website 工作流](https://github.com/hefrankeleyn/oryxos-practice/actions/runs/40469133260) 成功，浏览器实际打开 [线上 Hello 页面](https://oryxos.hifane.com/hello)，标题、固定说明和返回链接均正确。
- 课程作业迁入 `docs/homework/`，内部文档与图片引用采用相对路径；截图内容及原有答案保留。

本次未修改 Java 业务源码，也未重新执行 Maven 验证。第二周真实 Agent 运行与飞书修复使用的是另一个官方源码目录，相关证据记录在 `docs/homework/`，不代表当前练习仓库已实现 Runtime MVP。

## 核心阶段后续事项

1. 九工具与三个最终 Demo 的数量和范围已由宪章裁决，完整实现与场景验收仍未完成。
2. 本功能采用完整 Boot JAR 承载 Runtime、独立 CLI exec 保留轻量入口；完整核心阶段的命令与对外文档仍需后续同步。
3. 本功能只保证进程内会话，sessions 目录保留布局职责；审计独立落库，Session 持久化与恢复留后续功能。
4. 本功能已验证 MemoryService、最近默认 20 个完整轮投影和原始历史保留；Markdown 长期记忆及完整 Skill/Tool 联动留后续功能。
5. constitution 已采纳，002 的 spec/plan/tasks 和可控实现验证已建立；当前剩余 T065 真实 DeepSeek 验收。

## 建议实施顺序

```text
P0 规格收口与工程保障
  → US-1 Provider
  → US-2 ReAct
  → US-3 Memory 与 US-4 Tool（可并行）
  → US-5 Web / SQLite / CLI 补全
  → 三个端到端 Demo
```

首个功能的规格、打包和 Spec-Kit 产物已落实，003 已补充本地基础设施和 Java CI 配置；`LICENSE`、远端 CI 验证、完整 CLI/服务及其余核心能力仍按后续任务推进，不以本地验证结果代替。

## 更新规则

- 能力只有在实际实现、相关测试通过并具备对应 Demo 证据后，才能标记为“✅ 已实现”。
- 只有接口、POM、`package-info.java`、Mock 或文档时，最多标记为“🟡 部分实现”或“⬜ 未实现”。
- 能力状态变化时，在同一个提交或 PR 中更新本文；纯文案、注释、格式和配置调整不虚增实现进度。
- 本地构建、部署结果、线上运行、第三方回执和用户验收分别记录，不相互替代。
