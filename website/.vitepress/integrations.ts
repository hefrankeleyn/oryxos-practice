/**
 * 第三方集成配置：评论（Giscus）与访问统计（51.la）。
 *
 * 所有 ID 留空时，对应功能不会渲染 / 不会注入脚本，站点照常构建与发布。
 * 获取方式见 website/README.md「评论与统计」一节。
 */

/** Giscus 评论（基于 GitHub Discussions），挂在每篇文档页底部。 */
export const giscus = {
  /** 仓库，格式 owner/repo；必须是公开仓库并已开启 Discussions。 */
  repo: 'hefrankeleyn/oryxos-practice',
  /** giscus.app 生成的 data-repo-id，形如 R_kgDOxxxxxx。 */
  repoId: 'R_kgDOUx2nbg',
  /** Discussions 分类名，推荐 Announcements（只有维护者和 giscus 能新建讨论）。 */
  category: 'Announcements',
  /** giscus.app 生成的 data-category-id，形如 DIC_kwDOxxxxxx。 */
  categoryId: 'DIC_kwDOUx2nbs4DG5q_',
}

/** 51.la 访问统计（V6 JS SDK）。 */
export const la51 = {
  /** 51.la 后台「统计代码」里的 id。 */
  id: '',
  /** 51.la 后台「统计代码」里的 ck（通常与 id 相同）。 */
  ck: '',
}

/** Giscus 是否已配置完整。 */
export const giscusEnabled = Boolean(giscus.repo && giscus.repoId && giscus.category && giscus.categoryId)

/** 51.la 是否已配置完整。 */
export const la51Enabled = Boolean(la51.id && la51.ck)
