# OryxOS 主页（website/）

基于 [VitePress](https://vitepress.dev) 的项目主页，线上地址：<https://oryxos.hifane.com>。

## 本地开发

在**仓库根目录**执行（`package.json` 在根目录）：

```bash
npm install            # 首次安装依赖
npm run docs:dev       # 本地预览，默认 http://localhost:5173
npm run docs:build     # 构建，产物在 website/.vitepress/dist
npm run docs:preview   # 预览构建产物
```

## 目录结构

```text
website/
├── index.md                    # 中文首页（根路径）
├── docs/                       # 中文文档：what / quick-start / architecture / roadmap / faq
├── en/                         # 英文站（/en/），结构与中文一一对应
├── public/
│   ├── CNAME                   # 自定义域名 oryxos.hifane.com
│   ├── favicon.svg
│   └── images/                 # logo 与架构图（从 docs/images 复制，修改后两处同步）
└── .vitepress/
    ├── config.mts              # 站点配置：双语、导航、侧边栏、SEO
    ├── integrations.ts         # 评论（Giscus）与统计（51.la）的 ID，留空即关闭
    └── theme/
        ├── custom.css          # 全局主题（靛蓝→紫品牌色）
        └── components/
            ├── Home.vue        # 首页（中英双语，t('中文','English')）
            ├── Layout.vue      # 文档页底部挂评论
            └── GiscusComment.vue
```

新增文档时：中文放 `docs/`，英文放 `en/docs/`，并在 `config.mts` 两个语言的 `sidebar` 中各加一项。

## 发布

推送到 `main` 且改动涉及 `website/`、`package.json` 或部署工作流时，GitHub Actions（`.github/workflows/deploy-website.yml`）自动构建并发布到 GitHub Pages。

首次发布前需要完成两项一次性设置：

1. **GitHub Pages 来源**：仓库 Settings → Pages → Build and deployment → Source 选 **GitHub Actions**。
2. **域名解析**：在 `hifane.com` 的 DNS 服务商处添加一条记录：

   | 类型 | 主机记录 | 记录值 |
   |---|---|---|
   | CNAME | `oryxos` | `hefrankeleyn.github.io` |

   解析生效后，在 Settings → Pages → Custom domain 确认显示 `oryxos.hifane.com`，并勾选 **Enforce HTTPS**（证书签发可能需要几分钟到一小时）。

## 评论与统计

两项都在 `.vitepress/integrations.ts` 中配置，**ID 留空时功能自动关闭**，不影响构建和发布。

### Giscus 评论

Giscus 把每篇文档页的评论存为仓库 GitHub Discussions 里的一条讨论，访客用 GitHub 账号登录评论。

1. 确认仓库是**公开**的。
2. 仓库 Settings → General → Features，勾选 **Discussions**。
3. 打开 <https://github.com/apps/giscus>，点 **Install**，授权范围选 **Only select repositories** → `oryxos-practice`。
4. 打开 <https://giscus.app/zh-CN>，在「仓库」一栏填 `hefrankeleyn/oryxos-practice`，页面提示"成功！该仓库满足所有条件"即可。
5. 「页面 ↔ discussion 映射关系」选 **pathname**；「Discussion 分类」选 **Announcements**（只有维护者和 giscus 能新建讨论，避免被灌水）。
6. 页面下方「启用 giscus」会生成一段 `<script>`，从中复制：
   - `data-repo-id` → 填到 `giscus.repoId`
   - `data-category-id` → 填到 `giscus.categoryId`

### 51.la 统计

1. 在 <https://v6.51.la> 注册并登录。
2. 「添加站点」：名称填 OryxOS，域名填 `oryxos.hifane.com`。
3. 进入该站点的「统计代码」，找到类似 `LA.init({id:"XXXX",ck:"XXXX"})` 的一行，把 `id` 和 `ck` 分别填到 `la51.id`、`la51.ck`。
4. 发布后访问站点，几分钟后 51.la 后台即可看到数据。
