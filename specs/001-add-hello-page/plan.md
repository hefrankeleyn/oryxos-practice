# Implementation Plan: OryxOS Hello Spec Kit 页面

**Branch**: `homework/spec-kit-hello-world` | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-add-hello-page/spec.md`

**Status**: 规划与设计完成；页面实现、网站构建和浏览器验收尚未执行。

## Summary

后续实现仅新增 `website/hello.md`，在 `/hello` 展示规格指定的主标题、中文说明和返回中文首页
`/` 的链接。复用现有 VitePress 1.6.4 的文件路由、Markdown、导航和主题，不改配置或依赖。

采用页面级 `layout: page`，正文容器复用现有 `.vp-doc` 排版。
必要的留白与内容宽度只在本页用独立容器及 scoped 样式处理，不添加脚本或独立组件。
此布局无需修改全局主题即可避免附加文档评论、编辑入口与更新时间。
选择依据见 [research.md](research.md)，实际效果在实现后验证。

## Technical Context

**Language/Version**: Markdown 与 VitePress 支持的页面级 scoped CSS；当前本机 Node.js 26.5.0、
npm 11.17.0，仅作为环境记录，不新增版本要求。

**Primary Dependencies**: 现有 VitePress；根 `package.json` 声明 `^1.6.4`，
`package-lock.json` 与已安装版本均为 1.6.4。不得新增依赖或修改锁文件。

**Storage**: 不适用。没有数据库、实体、业务持久化或状态变更；
Markdown 是页面源文件，构建 HTML 是静态产物，见 [data-model.md](data-model.md)。

**Testing**: 现有 `npm run docs:build` 与 `npm run docs:preview`，
结合真实浏览器直接访问、刷新、返回首页、桌面与移动预览及全部既有中英文路由回归。
不新增测试框架，不为静态文案编写镜像实现的单元测试。

**Target Platform**: 现有网站支持的桌面与移动浏览器；本地构建产物预览，
保持既有强制浅色主题与中英文导航。

**Project Type**: VitePress 静态项目网站；不涉及 `oryxos-web` 的 Java Runtime 服务实现。

**Performance Goals**: 采用规格 SC-001 至 SC-004 的可读、可达与范围指标；
本练习没有额外的延迟、并发或吞吐目标。

**Constraints**: 后续业务源码仅新增 `website/hello.md`；保留首页、导航、全局主题、
既有文档与英文站点，不新增第三方依赖。本阶段只生成四份规划文档。

**Scale/Scope**: 1 个新增页面、3 项指定内容、9 条功能要求；回归现有 12 个页面路径，
覆盖桌面与移动两种展示方式。

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

依据 [constitution 1.0.0](../../.specify/memory/constitution.md)，
Ratified 与 Last Amended 均为 2026-10-14。规划前已读取规格与宪章；
下表的设计后结论针对本次设计文档，不代表页面或 Runtime 能力已经实现。

| 宪章检查项 | 规划前 | 设计后 | 依据与适用范围 |
|------------|--------|--------|----------------|
| I. JDK 21、Spring Boot 3.x、9 模块、同步与 virtual thread | 通过 | 通过 | 不改 Java、POM、模块或内部依赖；既有 Runtime 原则保持 |
| II. 自实现 ReAct、唯一 ToolExecutor、禁用自动 Tool、Provider 映射与统一入口 | 通过 | 通过 | 静态页面不新增或调用这些 Runtime 能力，不改变其边界 |
| III. Agent、Skill、Memory、Tool 与 Notify 归属 | 通过 | 通过 | 不新增 Agent 定义、记忆后端、工具或通知配置 |
| IV. 密钥、Sandbox 与敏感日志 | 通过 | 通过 | 固定公开文案与站内链接，不新增凭证、执行逻辑、第三方集成或日志 |
| V. LLM / Tool 审计与显式建表 | 不适用，原约束保留 | 不适用，原约束保留 | 没有 LLM / Tool 调用或数据库操作，不虚构审计表或迁移 |
| 核心阶段范围与实施顺序 | 通过 | 通过 | 属于用户明确指定的网站练习，不扩展 Runtime MVP 或打乱其 US 依赖顺序 |
| 最小充分验证与视觉核验 | 通过 | 通过 | 计划包含网站构建、产物预览、桌面/移动展示和既有中英文路由回归 |
| 保留原有改动与最小文件范围 | 通过 | 通过 | 本阶段四份文档；后续实现只新增一个页面文件 |
| 治理与交付状态 | 通过 | 通过 | 不改 constitution，不提交/推送/部署；区分规划、实现、构建和验收 |

**规划前 Gate**: PASS；适用规则没有冲突，不适用项已说明原因。

**设计后 Gate**: PASS；单文件页面设计、无新依赖、无后端契约，
验证方案覆盖全部 FR 与 SC，没有需要放宽的宪章条款。

## Project Structure

### Documentation (this feature)

```text
specs/001-add-hello-page/
├── spec.md                    # 既有规格，保持不变
├── checklists/requirements.md # 既有质量清单，保持不变
├── plan.md                    # 本阶段产物
├── research.md                # Phase 0：选型与仓库证据
├── data-model.md              # Phase 1：明确数据模型和后端接口不适用
└── quickstart.md              # Phase 1：实现后的验证指南
```

独立 `contracts/` 不生成：后端 API、协议与请求响应模型均不适用；
公开页面的界面约定已由规格 FR-001 至 FR-004 定义，直接引用即可。
本阶段不生成 `tasks.md`。

### Source Code (repository root)

```text
package.json                  # 现有构建/预览命令，不改
package-lock.json             # 现有依赖锁定，不改
website/
├── index.md                  # 中文首页，不改
├── hello.md                  # 唯一计划新增的页面，本阶段未创建
├── docs/                     # 现有中文文档，不改
├── en/                       # 现有英文首页和文档，不改
└── .vitepress/
    ├── config.mts            # base、cleanUrls、导航、侧栏，不改
    ├── integrations.ts       # 既有评论和统计配置，不改
    └── theme/                # 现有布局、组件和全局样式，不改
```

**Structure Decision**: 网站根目录的 Markdown 文件自然对应根级页面；
`website/hello.md` 对应 `/hello`，无需路由注册。
正文主标题、说明与站内链接使用 Markdown；容器保留 Markdown 所需的空行。
只允许页面自身的最少布局样式，选择器限定到独立容器并采用 scoped，
标题、颜色、字体和链接样式复用现有主题。
不复制首页布局，不新增 `/en/hello`，不修改文档侧栏或导航。

### 最小实施顺序（后续阶段）

1. 新增唯一页面文件，设置页面级布局与标题，填写规格固定文案和 `/` 返回链接；
   只添加本页必需的容器与留白/宽度样式，不使用脚本。
2. 按 [quickstart.md](quickstart.md) 执行构建，并预览构建产物；
   先验证 `/hello` 的首次直接访问、刷新、新标签访问、返回首页和两种展示方式。
3. 回归全部既有中英文路径及导航，复核源码/依赖差异并报告实际证据；
   构建、浏览器验收与部署状态分别记录。

### 要求与验证映射

| 规格要求 | 验证项 |
|----------|--------|
| FR-001、FR-002、FR-003 / SC-001 | H01：直接访问、新标签、刷新及固定文案核对 |
| FR-004 / SC-002 | H02：一次点击，在当前标签返回中文 `/` |
| FR-005 / SC-003 | R01：全部既有中英文路径、导航和展示；R02：文件范围 |
| FR-006、FR-007 / SC-004 | R02：仅一个新增页面，无英文页面、依赖或额外功能变更 |
| FR-008 / SC-004 | B01：现有构建成功；H01、R01：产物预览可访问 |
| FR-009 / SC-004 | H03：桌面与移动内容可读、链接可点、无样式外溢 |

## Complexity Tracking

无宪章违例，无新增架构或复杂度豁免。数据库实体、后端 API、路由配置、
自定义组件和新依赖均不需要；不保留模板中的示例违例。
