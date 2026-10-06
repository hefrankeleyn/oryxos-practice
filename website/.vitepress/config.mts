import { defineConfig, type HeadConfig } from 'vitepress'
import { la51, la51Enabled } from './integrations'

const SITE_URL = 'https://oryxos.hifane.com'
const REPO_URL = 'https://github.com/hefrankeleyn/oryxos-practice'

const DESC_ZH = '给 Agent 一个可掌控的运行环境。OryxOS 是 Java 原生的 Agent Harness OS：一个目录定义一个 Agent，一个底座提供共享执行环境。当前处于 Runtime MVP 工程骨架阶段。'
const DESC_EN = 'A home for your Agents, on your terms. OryxOS is a Java-native Agent Harness OS with file-based definitions and a shared runtime. Currently at the Runtime MVP engineering skeleton stage.'

/** 51.la 统计脚本：仅在 integrations.ts 中配置了 ID 时注入。 */
const analyticsHead: HeadConfig[] = la51Enabled
  ? [
      ['script', { charset: 'UTF-8', id: 'LA_COLLECT', src: '//sdk.51.la/js-sdk-pro.min.js' }],
      ['script', {}, `LA.init({id:"${la51.id}",ck:"${la51.ck}",autoTrack:true,hashMode:true})`],
    ]
  : []

export default defineConfig({
  title: 'OryxOS',
  titleTemplate: ':title — OryxOS',
  description: DESC_ZH,
  base: '/',
  cleanUrls: true,
  appearance: 'force-light',
  lastUpdated: true,
  // website/README.md 是维护说明，不作为页面发布
  srcExclude: ['README.md'],

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    ['meta', { name: 'theme-color', content: '#F4F5F2' }],
    ['meta', { name: 'author', content: 'hefrankeleyn' }],
    ['meta', { name: 'keywords', content: 'OryxOS, Agent OS, Agent Harness, AI Agent, Java, Spring AI, Spring AI Alibaba, MCP, ReAct, 私有部署, 企业级 Agent' }],
    ['meta', { name: 'robots', content: 'index, follow' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:site_name', content: 'OryxOS' }],
    ['meta', { property: 'og:title', content: 'OryxOS — 企业级 Agent 操作系统' }],
    ['meta', { property: 'og:description', content: DESC_ZH }],
    ['meta', { property: 'og:url', content: SITE_URL }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['link', { rel: 'canonical', href: SITE_URL }],
    ...analyticsHead,
  ],

  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      themeConfig: {
        nav: [
          { text: '首页', link: '/' },
          { text: '文档', link: '/docs/what' },
          { text: 'GitHub', link: REPO_URL },
        ],
        sidebar: {
          '/docs/': [
            {
              text: '快速入门',
              items: [
                { text: 'OryxOS 是什么', link: '/docs/what' },
                { text: '快速开始', link: '/docs/quick-start' },
              ],
            },
            {
              text: '深入了解',
              items: [{ text: '系统架构', link: '/docs/architecture' }],
            },
            {
              text: '参考',
              items: [
                { text: '路线图', link: '/docs/roadmap' },
                { text: '常见问题', link: '/docs/faq' },
              ],
            },
          ],
        },
        outline: { label: '本页目录', level: [2, 3] },
        docFooter: { prev: '上一页', next: '下一页' },
        lastUpdated: { text: '最后更新' },
        editLink: {
          pattern: `${REPO_URL}/edit/main/website/:path`,
          text: '在 GitHub 上编辑此页',
        },
        returnToTopLabel: '回到顶部',
        sidebarMenuLabel: '菜单',
        langMenuLabel: '切换语言',
      },
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      description: DESC_EN,
      themeConfig: {
        nav: [
          { text: 'Home', link: '/en/' },
          { text: 'Docs', link: '/en/docs/what' },
          { text: 'GitHub', link: REPO_URL },
        ],
        sidebar: {
          '/en/docs/': [
            {
              text: 'Getting Started',
              items: [
                { text: 'What is OryxOS', link: '/en/docs/what' },
                { text: 'Quick Start', link: '/en/docs/quick-start' },
              ],
            },
            {
              text: 'Deep Dives',
              items: [{ text: 'Architecture', link: '/en/docs/architecture' }],
            },
            {
              text: 'Reference',
              items: [
                { text: 'Roadmap', link: '/en/docs/roadmap' },
                { text: 'FAQ', link: '/en/docs/faq' },
              ],
            },
          ],
        },
        outline: { level: [2, 3] },
        editLink: {
          pattern: `${REPO_URL}/edit/main/website/:path`,
          text: 'Edit this page on GitHub',
        },
      },
    },
  },

  themeConfig: {
    logo: '/images/logo-mark.svg',
    siteTitle: 'OryxOS',
    socialLinks: [{ icon: 'github', link: REPO_URL }],
    search: { provider: 'local' },
  },

  sitemap: {
    hostname: SITE_URL,
  },
})
