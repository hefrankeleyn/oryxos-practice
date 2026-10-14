# Quickstart: Hello Spec Kit 页面验证

**Date**: 2026-10-14
**Feature**: [spec.md](spec.md)
**Status**: 后续实现阶段的验证指南；本规划阶段未执行构建或页面验收。

## 前置条件

- 后续实现已按 [plan.md](plan.md) 新增 `website/hello.md`，没有其他业务源码改动。
- 使用仓库现有 Node.js/npm 与已安装的网站依赖；依赖缺失时按 `website/README.md`
  准备现有依赖，不新增包、不改版本或锁文件。
- 实施前已记录 `git status` 和非目标文件基线，便于区分用户原有改动。
- 固定文案及界面行为以规格 FR-001 至 FR-004 为准；数据与后端接口不适用，
  见 [data-model.md](data-model.md)。

## B01：构建网站

按 constitution 的网站构建要求，在 `website/` 执行现有命令。
根 `package.json` 是该命令的定义来源；npm 会解析到仓库根。

```bash
cd "/Volumes/Work-Data/专业/11-AI/06-AI工程化实战/02-projects/oryxos-practice/website"
npm run docs:build
```

预期命令退出码为 0，构建产物位于 `website/.vitepress/dist/`，
其中包含新增页面的 `hello.html`。
记录实际结果；构建失败时先分析原因，不删除检查、跳过错误或修改无关页面以掩盖问题。

## 启动构建产物预览

在同一工作目录运行：

```bash
npm run docs:preview
```

默认访问地址为 [本地预览](http://localhost:4173/)。
如端口占用，停止当前预览后用 `npm run docs:preview -- --port 4174`，
并将下列地址中的端口一致替换；不修改站点配置或终止无关进程。
验证结束后关闭本次启动的预览进程。

## H01：直接访问、刷新与文案

1. 在新浏览器标签直接输入 [Hello 页面](http://localhost:4173/hello)，
   不先经过首页，不使用站内导航建立页面状态。
2. 核对可见主标题“Hello Spec Kit”、完整说明
   “这是通过 Spec Kit 工作流完成的第一个功能。”及“返回 OryxOS 首页”链接。
3. 在该地址刷新，三项内容仍应完整显示，没有 404 或空白页面。
4. 再次以新标签直接打开该地址，重复核对；正文使用正常标题、段落和链接样式，
   没有附加文档评论、编辑或更新时间区域。
5. 浏览器标题保持站点既有规则；不要求仅有正文主标题而去修改全局标题模板。

预期：规格 SC-001 两类验收场景均通过。不能仅以开发服务器中的客户端跳转或 HTTP 200
判断完整内容已验收。

## H02：返回中文首页

在 `/hello` 点击一次“返回 OryxOS 首页”。
预期当前标签进入 [中文首页](http://localhost:4173/)，URL 根路径为 `/`，
看到既有中文首页；不打开新标签、不进入英文站或文档页。核对 SC-002。

## H03：桌面与移动展示

使用现有浏览器的桌面与移动预览；可分别选择 1440×900 与 390×844 作为测试视口。
尺寸是本地核验示例，不新增产品断点要求。

两种展示方式都执行 H01 与 H02，确认标题清晰、说明完整、链接可见且可点击；
正文换行正常，不被导航遮挡，没有异常横向滚动。
本页容器只控制自身留白和宽度，颜色、字体与链接沿用现有主题。
记录两种视口的实际观察或截图，核对 FR-009 与 SC-004。

## R01：既有中英文路由与导航回归

在同一构建产物预览中逐一直接访问并刷新下列 12 个路径。
桌面与移动模式都核对页面内容与原有布局，没有 404、空白、样式外溢或导航变更。

| 页面 | 本地路径 |
|------|----------|
| 中文首页 | `/` |
| OryxOS 是什么 | `/docs/what` |
| 中文快速开始 | `/docs/quick-start` |
| 中文系统架构 | `/docs/architecture` |
| 中文路线图 | `/docs/roadmap` |
| 中文常见问题 | `/docs/faq` |
| 英文首页 | `/en/` |
| What is OryxOS | `/en/docs/what` |
| Quick Start | `/en/docs/quick-start` |
| Architecture | `/en/docs/architecture` |
| Roadmap | `/en/docs/roadmap` |
| FAQ | `/en/docs/faq` |

从两种语言首页使用既有首页/文档导航进入相应路径，
并核对既有语言切换到中英文首页的行为；不增加 Hello 导航项。
已有文档侧栏、评论和首页内容保持原有行为，英文站点不增加 `/en/hello`。
这些回归结果用于 SC-003；不对生产站点或评论服务写入数据。

## R02：差异范围与交付记录

从仓库根检查改动，并与实施前记录对照：

```bash
cd "/Volumes/Work-Data/专业/11-AI/06-AI工程化实战/02-projects/oryxos-practice"
git status --short --untracked-files=all
git diff --check
git diff -- website package.json package-lock.json
```

允许的业务源码新增仅为 `website/hello.md`。
首页、导航配置、全局主题、文档、英文站点、依赖与锁文件应与实施前一致；
未跟踪的新页面须结合文件内容和 `git status` 核对，不能只看 `git diff`。
本地生成的构建/缓存产物不作为需要提交的业务源码。
保留用户原有 `.agents/`、`.specify/` 与 `specs/` 工作内容，不暂存、提交、推送或部署。

交付时分别记录 B01、H01-H03、R01-R02 的结果以及未覆盖项。
页面实现、构建成功、本地浏览器验收与线上部署是独立状态；
本指南不包含上线或外部用户验收。
