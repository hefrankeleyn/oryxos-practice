# OryxOS 实施状态

> 最后核对：2026-10-03（Asia/Shanghai）
>
> 核对范围：当前本地工作树。本文只记录实现与验证状态，不替代需求文档、技术方案或路线图。

## 状态口径

| 状态 | 含义 |
|---|---|
| ✅ 已实现 | 已有实际代码，并完成与当前范围相称的本地验证 |
| 🟡 部分实现 | 只有骨架、局部能力或配置，尚未完成端到端验收 |
| ⬜ 未实现 | 只有目标设计、模块占位或文档描述，没有可用实现 |
| ⏸ 未启用 | 已预留接入点，但配置为空或明确暂不开启 |

“本地验证通过”不等于“已部署”，更不等于“线上用户验收通过”。部署、外部 Provider、GitHub/Giscus、真实设备或业务系统验收必须单独记录。

## 当前结论

OryxOS 当前完成了 **项目文档、9 模块 Maven 工程骨架、Spring Boot 启动入口、CLI 版本入口和双语项目网站**。Runtime MVP 的五大核心能力尚未进入实质实现阶段，不能按 README 和官网展示的 1.0 目标命令直接使用。

当前生产 Java 源码中，排除 `package-info.java` 后只有 4 个类：

- `oryxos-boot/.../OryxOsApplication.java`
- `oryxos-cli/.../OryxOsCliLauncher.java`
- `oryxos-cli/.../OryxOsCommand.java`
- `oryxos-cli/.../OryxOsVersionProvider.java`

当前有 2 个测试类、4 个测试用例：CLI 版本/帮助 3 个，Spring 上下文加载 1 个。

## 工程与交付物

| 项目 | 状态 | 当前证据 | 尚缺内容 |
|---|---|---|---|
| 六份项目资料 | ✅ 已实现 | `docs/01` 至 `docs/06` 已存在 | 仍需收口跨文档冲突 |
| 根目录 Agent 规则 | ✅ 已实现 | `AGENTS.md` 为共用规则，`CLAUDE.md` 为 Claude Code 入口 | 后续随架构决策维护 |
| 9 模块 Maven 骨架 | ✅ 已实现 | 根 POM 聚合 9 个功能/启动模块，能够编译测试和打包 | 各模块业务实现 |
| Spring Boot 入口 | 🟡 部分实现 | `OryxOsApplication.main`、工作区目录保障、上下文冒烟测试 | 实际 Service、Controller、数据库表和健康接口 |
| CLI 入口 | 🟡 部分实现 | 独立 exec JAR 支持 `--version`、`--help` 和无参提示 | 12 个业务命令及其集成 |
| 项目网站 | ✅ 已实现 | VitePress 中英文首页与 5 类文档页；本地构建通过 | 线上可达性和发布结果需独立验收 |
| GitHub Pages 工作流 | 🟡 部分实现 | `.github/workflows/deploy-website.yml` 存在 | 本文件不证明最近一次线上部署成功 |
| Giscus 评论 | 🟡 部分实现 | 仓库 ID 和 Announcements 分类 ID 已写入 `integrations.ts` | 部署后登录、创建 Discussion、回显评论的真实验收 |
| 51.la 统计 | ⏸ 未启用 | `id`、`ck` 保持为空，代码会自动关闭 | 用户决定是否开通并提供配置 |
| Apache 2.0 许可证文件 | ⬜ 未实现 | README 与 POM 已声明 Apache 2.0 | 根目录缺少 `LICENSE` 正文 |
| Java/网站持续集成 | ⬜ 未实现 | 当前只有网站部署 workflow | 增加 PR/Push 构建测试 workflow |
| Spec-Kit artifacts | ⬜ 未实现 | 当前没有 `.specify/` | constitution、spec、plan、tasks |

## Runtime MVP 核心能力

| 能力 | 状态 | 当前证据 | 完成判定 |
|---|---|---|---|
| US-1 Provider | ⬜ 未实现 | `oryxos-provider` 只有 POM 和 `package-info.java` | 显式 provider name 映射；至少一个真实或可控测试 Provider 调用通过；LLM 审计落库 |
| US-2 ReAct | ⬜ 未实现 | `oryxos-core` 只有 POM 和 `package-info.java` | `ReActLoop`、`PromptBuilder`、`ToolExecutor`、`AgentService` 实现；多轮 Tool 调用和迭代上限测试通过 |
| US-3 Memory | ⬜ 未实现 | `oryxos-memory` 只有 POM 和 `package-info.java` | 会话记忆与 `MarkdownMemoryStore` 实现；保存、检索、跨会话 Demo 通过 |
| US-4 Tool / MCP / Sandbox | ⬜ 未实现 | `oryxos-tool` 只有 POM 和 `package-info.java` | 9 个内置 Tool、统一注册、MCP stdio、白名单 Sandbox 与 Tool 审计实现并测试 |
| US-5 REST API | ⬜ 未实现 | `oryxos-web` 只有 POM 和 `package-info.java` | 10 个核心端点、统一响应/异常、超时与输入限制测试通过 |
| SQLite 业务持久化 | ⬜ 未实现 | 依赖和 datasource 配置存在，但没有表脚本、实体或 Repository | 六张核心表可初始化；Session、审计、任务和通知数据可读写 |
| CLI Channel | ⬜ 未实现 | `oryxos-channel-cli` 只有 POM 和 `package-info.java` | `oryxos chat` 多轮会话可用，并复用 `AgentService` |
| 12 个 CLI 命令 | ⬜ 未实现 | 顶层命令仅支持版本和帮助 | `init`、`status`、`chat`、`serve`、`gateway`、profile/provider/tool/session 命令全部测试通过 |
| Agent 加载与上下文 | ⬜ 未实现 | 仅有文档设计 | `AGENT.md` 解析、Profile 派生、Bootstrap 与 Skill 渐进式披露测试通过 |
| AgentScheduler | ⬜ 未实现 | 仅有文档设计 | cron 触发、防重入、执行记录和同链路调用测试通过 |
| 三个验收 Demo | ⬜ 未实现 | 仅有需求和技术方案描述 | 每日天气、每日科技日报、每日 GitHub 日报分别保留可重复运行证据 |

## 最近一次本地验证

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

## 开发前待收口事项

以下问题在进入 US-1 前应形成唯一决定，并回写需求/技术文档或 ADR：

1. 统一需求、技术方案和编程指南中的内置 Tool 数量及三个验收 Demo。
2. 确定最终可执行包边界：单个 `oryxos.jar` 同时承载 CLI 与服务，还是服务 JAR 与 CLI JAR 分离，并同步修正 README。
3. 统一 Session 的 SQLite 存储位置与 `.oryxos/sessions/` 目录职责。
4. 统一 Memory 的注入、核心区/归档区和截断规则。
5. 创建并确认 Spec-Kit constitution、spec、plan 和 tasks。

## 建议实施顺序

```text
P0 规格收口与工程保障
  → US-1 Provider
  → US-2 ReAct
  → US-3 Memory 与 US-4 Tool（可并行）
  → US-5 Web / SQLite / CLI 补全
  → 三个端到端 Demo
```

P0 至少包括：解决上面的文档冲突、确定可执行包契约、补 `LICENSE`、增加 CI，并建立 Spec-Kit artifacts。

## 更新规则

- 能力只有在实际实现、相关测试通过并具备对应 Demo 证据后，才能标记为“✅ 已实现”。
- 只有接口、POM、`package-info.java`、Mock 或文档时，最多标记为“🟡 部分实现”或“⬜ 未实现”。
- 能力状态变化时，在同一个提交或 PR 中更新本文；纯文案、注释、格式和配置调整不虚增实现进度。
- 本地构建、部署结果、线上运行、第三方回执和用户验收分别记录，不相互替代。
