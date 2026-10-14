# Research: OryxOS Hello Spec Kit 页面

**Date**: 2026-10-14
**Feature**: [spec.md](spec.md)
**Status**: 仓库与依赖源码核对完成；没有待澄清项。以下为设计结论，尚未运行页面验收。

## 1. 页面位置与路由

**Decision**: 后续只新增 `website/hello.md`，沿用现有根路径和 clean URL 设置。

**Rationale**: 根 `package.json` 的开发、构建和预览命令均以 `website` 为站点根。
当前 `website/index.md` 为中文首页，`website/en/index.md` 为英文首页；
中文文档位于 `website/docs/`，英文文档位于 `website/en/docs/`。
`website/.vitepress/config.mts:22-23` 已配置 `base: '/'` 与 `cleanUrls: true`。
当前没有 `website/hello.md`，新增根级文件即可形成独立 `/hello`，无需改全局配置。

**Alternatives considered**: 放在 `docs/` 会形成文档路由；
新增 `en/` 页面超出范围；自定义路由、Vue Router 或后端 Controller 都无必要。

## 2. 页面布局与样式

**Decision**: 使用页面级 `layout: page`；固定标题、说明与链接使用 Markdown。
正文容器复用 `.vp-doc`，仅以本页 scoped 样式设置必要的留白与内容宽度。

**Rationale**: 当前 VitePress 1.6.4 的
`node_modules/vitepress/dist/client/theme-default/components/VPContent.vue:24-27`
将 page 布局交给 `VPPage`；
`VPPage.vue:1-6` 仅渲染内容与 page 插槽，不消费 `doc-after` 或文档 footer。
现有导航仍由默认主题外围布局提供。

默认 doc 布局在 `VPDoc.vue:54-57` 渲染文档 footer 和 `doc-after`。
仓库 `website/.vitepress/theme/components/Layout.vue:4-5` 在该插槽挂 Giscus，
而 `website/.vitepress/integrations.ts` 当前已启用评论；没有现成的页面级评论关闭开关。
使用 page 布局可在不修改该主题文件的前提下保持练习内容最小。

page 的裸 `Content` 不会自动附加文档样式或文档留白。
VitePress 的 `styles/components/vp-doc.css:16-20、78-101` 提供现成的标题与段落排版，
仓库 `custom.css:39、48` 提供链接及焦点样式。
因此复用 `.vp-doc`，以独立容器的 scoped 样式补足页面留白，
不新建全局字体、配色、选择器或组件。该判断仍需实现后的桌面与移动预览确认。

**Alternatives considered**: 默认 doc + 纯 Markdown 文件最短，但会继承评论与编辑区域；
修改全局 Layout 增加评论开关违背约束；裸 page 缺少正文排版与留白；
`layout: false`、首页布局或自定义组件会引入不必要的结构变化。

## 3. 标题与返回首页

**Decision**: 正文使用规格指定的一个主标题、完整中文说明和“返回 OryxOS 首页”链接；
链接目标为 `/`，不指定新标签打开。页面标题元数据沿用站点标题规则。

**Rationale**: `spec.md` 的 FR-002 至 FR-004 已明确文案和链接行为。
站点 `config.mts:20` 使用 `:title — OryxOS`，无需覆盖全局标题模板。
根路径指向既有中文首页；根级新页不匹配 `/docs/` 或 `/en/docs/` 侧栏。

**Alternatives considered**: 返回英文首页、添加导航入口、覆盖全局标题模板或新建英文对应页
均超出规格。

## 4. 依赖、数据模型与接口

**Decision**: 保持现有 VitePress 和锁文件；没有数据模型、业务状态、持久化或后端 API。
不生成重复的独立 contracts 文件，界面约定直接引用规格 FR-001 至 FR-004。

**Rationale**: 根依赖声明为 `vitepress: ^1.6.4`，锁定和实际安装版本均为 1.6.4。
固定静态内容不需要 Java 服务、SQLite、请求响应 schema 或认证流程。
当前源码已足以确定布局和路由行为，无需升级依赖或引用其他版本的接口。

**Alternatives considered**: 数据实体、迁移、接口描述文件或新测试依赖会扩大单页练习，
且不能增加本次验收覆盖。

## 5. 直接访问、刷新与预览

**Decision**: 实现后执行 `npm run docs:build`，再用 `npm run docs:preview`
验证构建产物中的直接 `/hello`、刷新和新标签访问；开发热更新不能替代产物预览。

**Rationale**: 根 `package.json:9-10` 定义现有构建与预览入口。
VitePress 1.6.4 的 `dist/node/chunk-D3CUZ4fa.js:49483-49496` 仍生成 `hello.html`。
其预览实现（同文件 `:53156-53177`）使用 sirv 服务构建产物；
sirv 默认尝试 HTML 扩展，并为请求构造无扩展、index 与扩展文件候选
（同文件 `:52966-52979、53075`），因此新页面构建后可由 `/hello` 直接读取。
实际可访问性仍须浏览器核验，不以源码分析替代验收。

预览默认端口为 4173（同文件 `:53157`）。
现有 `website/README.md` 建议从仓库根运行命令；
宪章要求在 `website/` 执行网站构建，本机 `npm prefix` 在该目录解析到仓库根，
两种目录入口使用同一份 `package.json`。验证指南按宪章从 `website/` 启动。

**Alternatives considered**: 只打开开发服务器中的站内链接不能证明直接请求与刷新；
新增重写或部署配置没有必要，本次也不进行远程部署验收。

## 6. 最小充分验证与宪章

**Decision**: 只规划现有网站构建、产物预览、两种展示方式和 12 个既有页面的回归；
不安排 Maven 构建或静态文案单元测试。规划前与设计后检查结果记录在
[plan.md](plan.md) 的 Constitution Check。

**Rationale**: 本功能不改 Java、POM、Agent Runtime、LLM、Tool、数据库或凭证。
审计规则针对实际 LLM / Tool 调用，本静态页面没有相应调用，不需要虚构审计逻辑。
网站内容与视觉变更按宪章执行网站构建和实际预览；
需求 FR-005 要求原有中英文页面与导航保持不变，因此必须做本地路由回归。

**Alternatives considered**: 全仓库 Java 测试、外部 Provider 验收、数据库 fixture、
新建自动化测试套件或部署网站均超出此次单页练习和规划阶段。

## 证据边界

本次只读取仓库文件与已安装版本的源码，确认了设计可采用的接口和组织方式。
新增页面还未创建，构建、浏览器预览、移动展示和路由回归都仍待后续实现阶段执行；
既有站点历史构建记录不能作为新页面通过的证据。
