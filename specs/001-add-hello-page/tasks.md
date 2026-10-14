---
description: "Hello Spec Kit 单页功能的最小顺序任务清单"
---

# Tasks: OryxOS Hello Spec Kit 页面

**Input**: Design documents from `specs/001-add-hello-page/`

**Prerequisites**: [spec.md](spec.md)、[plan.md](plan.md)、[research.md](research.md)、
[data-model.md](data-model.md)、[quickstart.md](quickstart.md)，以及已采纳的
[constitution 1.0.0](../../.specify/memory/constitution.md)。

**Status**: 仅生成任务清单，全部任务未执行、未勾选。
本阶段只新增 `specs/001-add-hello-page/tasks.md`，不编写页面、不构建、不启动预览或浏览器验收。

**Tests**: 使用现有网站构建与真实浏览器验证；不新增自动化测试代码或测试框架。

**Organization**: 唯一 P1 用户故事为 US1“阅读练习页面并返回首页”；
8 项任务全部归属 US1，其中 1 项页面实现、7 项顺序检查与验证。

## Format: 任务编号、故事标签与准确文件路径

每项任务使用未勾选复选框、连续编号和 US1 标签；所有任务按编号顺序执行。
本流程没有可独立并行的任务，不添加并行标记。

## Path Conventions

文件路径均相对仓库根目录。命令的工作目录按任务说明选择；
在 `website/` 运行 npm 时，使用根 `package.json` 的现有网站命令。

后续实现唯一允许新增的业务源码为 `website/hello.md`。
不新增依赖、组件、后端接口、数据模型、路由配置或自动化测试框架；
首页、导航、主题、既有文档和英文站点保持实施前状态。

执行 T001 前记录当前 `git status` 与非目标文件/页面基线，保留用户原有改动。
这是编辑前保护操作，不另设初始化任务，不生成额外基线文件。
数据模型与后端 API 不适用，界面约定直接使用规格 FR-001 至 FR-004。
本任务序列沿用 plan.md 已通过的宪章检查，不更改宪章或 Runtime 架构。

## Phase 1: Setup (Shared Infrastructure)

不需要任务。仓库已有 VitePress 网站、现有依赖与构建/预览入口；
沿用现有环境，不初始化项目、安装新依赖或配置新工具。

## Phase 2: Foundational (Blocking Prerequisites)

不需要任务。本功能是独立静态页面，不依赖新增数据库、认证、后端服务、组件或路由基础设施。
无需创建任何实体、迁移、接口或 contracts 文件。

## Phase 3: User Story 1 - 阅读练习页面并返回首页 (Priority: P1) — MVP

**Goal**: 访客直接访问 `/hello`，阅读指定标题和说明，
并一次点击返回现有中文首页 `/`；其他页面与站点行为不变。

**Independent Test**: 对构建产物进行本地预览，在新标签直接打开并刷新 `/hello`，
核对三项指定内容，点击返回中文首页；在桌面与移动展示下重复核验，
再回归下方列明的 12 个既有中英文路径。全部通过才完成 US1 验收。

### Implementation for User Story 1

- [X] T001 [US1] 一次编辑新增 `website/hello.md`：采用 `layout: page` 与页面标题元数据，正文主标题为“Hello Spec Kit”，说明为“这是通过 Spec Kit 工作流完成的第一个功能。”，提供“返回 OryxOS 首页”链接到 `/` 并在当前标签打开；使用 Markdown、复用 `.vp-doc` 的独立容器及仅本页必要的 scoped 留白/宽度样式，不添加脚本、组件或全局样式。

### Validation for User Story 1（严格在实现后依次执行）

- [X] T002 [US1] 核对源码范围：与 T001 前基线对比 `website/hello.md`、`website/index.md`、`website/docs/`、`website/en/`、`website/.vitepress/`、`package.json` 和 `package-lock.json`；在仓库根执行 `git status --short --untracked-files=all`、`git diff --check` 及 `git diff -- website package.json package-lock.json`，结合新文件内容确认业务源码仅新增 `website/hello.md`，非目标文件和依赖未变，用户原有改动保留。
- [X] T003 [US1] 在 `website/` 执行现有 `npm run docs:build`，核对根 `package.json` 的构建入口，要求退出码为 0 且产物包含 `website/.vitepress/dist/hello.html`；记录构建结果，失败时先定位原因，不跳过错误或修改无关源码，构建成功只代表构建状态通过。
- [X] T004 [US1] 在 `website/` 执行 `npm run docs:preview`，启动 `website/.vitepress/dist/hello.html` 所属构建产物的本地预览，记录本次进程与实际端口，默认地址为 `http://localhost:4173/`；端口占用时按 `specs/001-add-hello-page/quickstart.md` 使用 `--port 4174`，不修改配置或终止无关进程，后续统一使用同一预览。
- [X] T005 [US1] 按 `specs/001-add-hello-page/quickstart.md` H01，在新标签直接打开本地 `/hello`、刷新并再次以新标签直接访问，核对 `website/hello.md` 的主标题、完整说明和返回链接均正确；确认没有 404、空白、附加评论/编辑/更新时间区域，浏览器标题沿用站点既有规则，不能只凭 HTTP 200 或开发服务器跳转判定通过。
- [X] T006 [US1] 按 `specs/001-add-hello-page/quickstart.md` H02，在本地 `/hello` 点击一次 `website/hello.md` 中的“返回 OryxOS 首页”链接，确认当前标签到达 `/` 并展示 `website/index.md` 的既有中文首页，不进入英文站或文档、不打开新标签。
- [X] T007 [US1] 按 `specs/001-add-hello-page/quickstart.md` H03，对 `website/hello.md` 的本地预览分别核验桌面与移动展示，可采用 1440×900 和 390×844 视口；重复直接访问、刷新与返回首页，确认标题和说明完整可读、链接可点、无导航遮挡或异常横向滚动，记录观察或截图且不增加仓库源码文件。
- [X] T008 [US1] 按 `specs/001-add-hello-page/quickstart.md` R01 及下方矩阵，在桌面与移动预览中逐一直接访问并刷新 12 个既有路径，核对对应 `website/index.md`、`website/docs/`、`website/en/index.md`、`website/en/docs/` 的内容、导航、语言切换、侧栏与原有主题不变；复核范围后关闭仅本次启动的预览，分别汇报页面实现、构建、浏览器验收及未覆盖项，部署明确为不在本流程内且未执行，不提交、不推送、不部署。

**Checkpoint**: T001 完成表示页面源码完成；T003 完成表示构建通过；
T005-T008 全部完成并具备实际浏览器证据才表示 US1 本地验收通过。
本清单生成时这些任务全部保持未勾选。

### T008：既有路径与源码回归矩阵

以下文件只读核验，不修改。全部路径均在 T004 的同一构建产物预览中访问。

| 既有路径 | 对应源码 |
|----------|----------|
| `/` | `website/index.md` |
| `/docs/what` | `website/docs/what.md` |
| `/docs/quick-start` | `website/docs/quick-start.md` |
| `/docs/architecture` | `website/docs/architecture.md` |
| `/docs/roadmap` | `website/docs/roadmap.md` |
| `/docs/faq` | `website/docs/faq.md` |
| `/en/` | `website/en/index.md` |
| `/en/docs/what` | `website/en/docs/what.md` |
| `/en/docs/quick-start` | `website/en/docs/quick-start.md` |
| `/en/docs/architecture` | `website/en/docs/architecture.md` |
| `/en/docs/roadmap` | `website/en/docs/roadmap.md` |
| `/en/docs/faq` | `website/en/docs/faq.md` |

## Phase 4: Polish & Cross-Cutting Concerns

不需要额外任务。源码范围检查由 T002 覆盖，视觉与回归由 T007-T008 覆盖，
预览关闭与交付状态记录纳入 T008。不得追加无关清理、重构、文档改写或部署步骤。

## Dependencies & Execution Order

### Phase Dependencies

Setup 与 Foundational 均为空阶段，US1 可在现有环境中开始。
最后阶段无需独立操作，US1 验证完成即结束本流程。

### User Story Dependencies

只有 US1（P1），没有其他故事依赖；整个功能与 MVP 范围一致。

### Within Each User Story

```text
T001 页面编辑
  -> T002 源码范围检查
  -> T003 网站构建
  -> T004 构建产物预览
  -> T005 /hello 直接访问与刷新
  -> T006 返回中文首页
  -> T007 桌面与移动展示
  -> T008 12 路径回归、关闭预览与交付记录
```

任务仅在前一项达到预期结果后继续。源码如需返修，只允许修改 `website/hello.md`；
返修后重新核对范围与构建，重启当前预览并重新执行受影响的浏览器验证。
不能用旧产物或旧验收结果替代新源码的结果。

### Parallel Opportunities

无。用户要求这组检查依次执行，构建、产物预览与浏览器核验有前后依赖，
且共用一个页面文件和同一预览状态；所有任务均不标记并行。

## Parallel Example: User Story 1

本故事没有并行执行示例。执行上方唯一顺序链，不同时编辑页面、构建和切换预览，
不将同一次页面编辑拆成多个执行者的工作。

## Implementation Strategy

### MVP First (User Story 1 Only)

完成 T001 的单文件实现后，依次完成 T002-T008。
US1 既是完整功能，也是本次 MVP；不增加其他用户故事、接口或基础设施。

### Incremental Delivery

仅按源码完成、构建通过、本地浏览器验收通过三个状态推进。
本流程没有部署或发布阶段，构建通过不能替代浏览器验收。

| 状态 | 完成依据 | 当前状态 |
|------|----------|----------|
| 页面源码 | T001 | 未执行 |
| 源码范围 | T002 | 未执行 |
| 网站构建 | T003 | 未执行 |
| 构建产物预览 | T004 | 未执行 |
| 本地浏览器验收 | T005-T008 的实际结果 | 未执行 |
| 部署 | 不在本流程范围 | 未执行 |

## Notes

- 本阶段只生成本文件，8 项任务均保持未勾选；任务生成不表示任何实现或验收完成。
- T001 覆盖 FR-001 至 FR-004；T002、T008 覆盖 FR-005 至 FR-007；
  T003-T005、T008 覆盖 FR-008；T007 覆盖 FR-009。
- T005 对应 SC-001，T006 对应 SC-002，T008 对应 SC-003，
  T002-T004、T007 对应 SC-004。
- 不新增依赖、组件、后端接口、数据模型、路由配置或自动化测试框架。
- 不修改 constitution、既有设计文档或用户执行记录；保留用户原有未提交改动。
- 不暂存、不提交、不推送、不部署，不连接生产系统完成这些本地检查。
