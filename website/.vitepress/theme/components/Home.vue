<script setup>
import { computed, ref } from 'vue'
import { useData, withBase } from 'vitepress'
import RuntimeScene from './RuntimeScene.vue'

const REPO_URL = 'https://github.com/hefrankeleyn/oryxos-practice'
const { lang } = useData()
const isZh = computed(() => lang.value === 'zh-CN')
const t = (zh, en) => (isZh.value ? zh : en)
const link = (path) => withBase(isZh.value ? path : `/en${path}`)
const selectedId = ref('weather')
const examples = computed(() => [
  {
    id: 'weather', tab: t('每日天气', 'Weather'),
    title: t('从天气数据，到出门建议。', 'From the forecast to a useful morning note.'),
    description: t('每天早上查天气、给出穿搭建议，发送到指定通知渠道。业务意图写在文件里，调度与执行由运行时承担。', 'Fetch the forecast, suggest what to wear and send it to a named notification channel. The file describes the job; the runtime handles scheduling and execution.'),
    tools: '[http_get, notify]',
    message: t('查询北京今天的天气并给出穿搭建议', 'Check today’s weather in Beijing and suggest what to wear'),
    body: t('查询北京今天的温度、降水与风力。\n给出简短的穿搭和出行建议。\n通过 notify 发送到 team-lark 渠道。', 'Get today’s temperature, rain and wind in Beijing.\nWrite a short note on clothing and travel.\nSend it to the team-lark channel using notify.'), binding: '',
    note: t('通知渠道按名称引用；连接配置与凭证不写进 Agent 正文。', 'Refer to notification channels by name; keep connection settings and credentials outside the Agent body.'),
  },
  {
    id: 'tech-digest', tab: t('科技日报', 'Tech digest'),
    title: t('任务不同，格式可以共用。', 'Different jobs. One shared editorial skill.'),
    description: t('汇总 AI 与芯片新闻，用绑定的日报 Skill 统一组稿格式，再读取长期记忆中的关注方向。Skill 正文按需加载。', 'Summarize AI and chip news, use a linked digest Skill for the format, and consult long-term memory for interests. Read the full Skill only when needed.'),
    tools: '[http_get, read_file, recall_memory, notify]',
    message: t('整理今天值得关注的科技新闻', 'Prepare today’s technology digest'),
    body: t('优先关注 AI 与芯片领域的新闻。\n需要组稿格式时读取 skills/digest/SKILL.md。\n用 recall_memory 查询关注方向，再发送到 team-lark。', 'Prioritize news about AI and chips.\nRead skills/digest/SKILL.md when you need the format.\nUse recall_memory for interests, then send to team-lark.'),
    binding: 'skills/\n      └── digest → ../../../skills/digest',
    note: t('Skill 是上下文，不是 Tool；每轮只注入名称、描述和读取路径。', 'A Skill is context, not a Tool. Each turn receives its name, description and read path—not its entire body.'),
  },
  {
    id: 'github-digest', tab: t('GitHub 日报', 'GitHub digest'),
    title: t('脚本做采集，Agent 做判断。', 'Let a script collect. Let the Agent interpret.'),
    description: t('用随 Agent 分发的脚本采集项目数据，再筛选、归纳和推送。确定性工作交给脚本，模型负责理解结果。', 'Bundle a script to collect repository data, then select, summarize and send the findings. The script does the deterministic work; the model interprets the result.'),
    tools: '[shell, notify]',
    message: t('整理今天值得关注的 GitHub 项目', 'Prepare today’s GitHub repository digest'),
    body: t('使用 shell 调用白名单内的 scripts/trending.sh。\n根据脚本输出挑选值得关注的 AI 项目。\n说明项目用途和关注理由，发送到 team-lark。', 'Use shell to run the allow-listed scripts/trending.sh.\nSelect noteworthy AI projects from the script output.\nExplain what each does and send the digest to team-lark.'),
    binding: 'scripts/\n      └── trending.sh',
    note: t('Shell 直传可执行文件与参数数组；应用层白名单不等于强隔离。', 'Shell passes an executable and argument array directly. An application allow-list is not strong isolation.'),
  },
])
const activeExample = computed(() => examples.value.find((item) => item.id === selectedId.value))
const agentDefinition = computed(() => `---
name: ${activeExample.value.id}
provider:
  name: deepseek
  model: deepseek-chat
tools: ${activeExample.value.tools}
schedules:
  - key: morning
    cron: "0 0 8 * * *"
    zone: Asia/Shanghai
    message: ${activeExample.value.message}
---

${activeExample.value.body}`)

/** 目标示例只切换展示，不发送网络请求或模拟真实执行。 */
function selectWithKeyboard(event, index) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const count = examples.value.length
  const next = event.key === 'Home' ? 0 : event.key === 'End' ? count - 1
    : (index + (event.key === 'ArrowRight' ? 1 : -1) + count) % count
  selectedId.value = examples.value[next].id
  event.currentTarget.parentElement.querySelectorAll('[role="tab"]')[next].focus()
}

const capabilities = computed(() => [
  { name: t('模型接入', 'Models'), detail: t('显式映射 Provider，厂商差异留在适配层。', 'Explicit Provider mapping keeps vendor differences in the adapter layer.'), contract: 'ProviderService' },
  { name: t('推理循环', 'Reasoning'), detail: t('自实现 ReAct；工具由运行时调度，不交给模型 SDK 自动执行。', 'An in-house ReAct loop. The runtime schedules tools—not the model SDK.'), contract: 'ReActLoop' },
  { name: t('上下文与记忆', 'Context & memory'), detail: t('会话与长期记忆分开管理；长期记忆使用可读的 Markdown。', 'Separate sessions from long-term memory. Keep long-term memory in readable Markdown.'), contract: 'MemoryService' },
  { name: t('工具与约束', 'Tools & boundaries'), detail: t('内置工具、MCP 与 Java 扩展统一接入，执行前校验，失败也审计。', 'Built-ins, MCP and Java extensions share one interface. Check before execution; audit failures too.'), contract: 'OryxTool' },
  { name: t('业务接入', 'Entry points'), detail: t('CLI、REST 与定时任务共用一个入口，不重复实现 Agent 链路。', 'CLI, REST and schedules share one entry point rather than separate Agent implementations.'), contract: 'AgentService' },
])
</script>

<template>
  <div class="oryx-home" :class="{ 'is-en': !isZh }">
    <section class="hero page-width" aria-labelledby="hero-title">
      <div class="hero-copy">
        <a class="stage-label" :href="`${REPO_URL}/blob/main/docs/IMPLEMENTATION_STATUS.md`" target="_blank" rel="noopener noreferrer"><span class="stage-dot" aria-hidden="true" />{{ t('Runtime MVP · 工程骨架阶段', 'Runtime MVP · Engineering skeleton') }}</a>
        <h1 id="hero-title"><template v-if="isZh">给 Agent 一个<br />可掌控的<br />运行环境。</template><template v-else>A home for<br />your Agents.<br />On your terms.</template></h1>
        <p class="hero-description">{{ t('OryxOS 是 Java 原生的 Agent Harness OS。一个目录定义一个 Agent，一个底座承载模型、记忆与工具，让业务意图与运行机制各归其位。', 'OryxOS is a Java-native Agent Harness OS. Define an Agent in a directory; share a runtime for models, memory and tools. Keep business intent separate from execution.') }}</p>
        <div class="hero-actions">
          <a class="button button-primary" :href="link('/docs/what')">{{ t('认识 OryxOS', 'Meet OryxOS') }}</a>
          <a class="button button-secondary" :href="REPO_URL" target="_blank" rel="noopener noreferrer"><svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="M12 2a10 10 0 0 0-3.16 19.49c.5.09.68-.22.68-.48v-1.86c-2.78.6-3.37-1.18-3.37-1.18-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.61.07-.61 1 .07 1.53 1.03 1.53 1.03.89 1.53 2.34 1.09 2.91.83.09-.64.35-1.09.64-1.34-2.22-.25-4.56-1.11-4.56-4.95 0-1.09.39-1.99 1.03-2.69-.1-.25-.45-1.27.1-2.65 0 0 .84-.27 2.75 1.03A9.57 9.57 0 0 1 12 6.81c.85 0 1.7.12 2.5.34 1.91-1.3 2.75-1.03 2.75-1.03.55 1.38.2 2.4.1 2.65.64.7 1.03 1.6 1.03 2.69 0 3.85-2.34 4.7-4.57 4.94.36.31.68.92.68 1.85v2.76c0 .26.18.58.69.48A10 10 0 0 0 12 2Z" /></svg>{{ t('查看源码', 'View source') }}</a>
        </div>
        <p class="hero-honesty">{{ t('现在可以构建工程与运行 CLI 版本入口。核心运行时正在开发，尚不可用于生产。', 'Build the project and run the CLI version entry today. The core runtime is in development—not ready for production.') }}</p>
      </div>
      <figure class="hero-figure"><RuntimeScene :is-zh="isZh" /><figcaption><span>{{ t('一个目录，一份职责。', 'One directory. One responsibility.') }}</span><span>{{ t('目标形态示意', 'Target design') }}</span></figcaption></figure>
      <div class="hero-foundation" :aria-label="t('技术与部署方向', 'Technology and deployment direction')"><span><b>Java 21</b>{{ t('原生运行时', 'Native runtime') }}</span><span><b>AGENT.md</b>{{ t('文件即定义', 'File-based definitions') }}</span><span><b>{{ t('私有部署', 'Self-hosted') }}</b>{{ t('运行在自己的基础设施', 'Your own infrastructure') }}</span></div>
    </section>

    <section class="idea-section page-width" aria-labelledby="idea-title">
      <div class="section-intro"><span class="section-mark" aria-hidden="true"><svg viewBox="0 0 32 32"><path d="M6 8h20v16H6zM6 14h20M12 14v10" fill="none" stroke="currentColor" stroke-width="1.6" /></svg></span><h2 id="idea-title">{{ t('不是再造一个\n聊天窗口。', 'Not another\nchat window.') }}</h2></div>
      <div class="idea-copy">
        <p class="lead">{{ t('真正需要被补齐的，\n是 Agent 的运行环境。', 'The missing piece is the environment\nyour Agents run in.') }}</p>
        <p>{{ t('模型知道如何回答，并不意味着 Agent 知道如何持续工作。上下文如何续、工具由谁执行、一次失败留下什么记录——这些问题不该在每个业务项目里重做一遍。', 'A model that can answer is not yet an Agent that can keep working. Carrying context, executing tools and recording failures should not be rebuilt in every business project.') }}</p>
        <div class="responsibility-split"><div><span>{{ t('业务方定义', 'You define') }}</span><p>{{ t('要做什么、使用哪些工具、遵循哪些规则。', 'The job, the available tools and the rules to follow.') }}</p></div><div><span>{{ t('底座负责 · 目标能力', 'Runtime handles · Target') }}</span><p>{{ t('模型调用、推理循环、记忆、调度与审计。', 'Model calls, reasoning, memory, scheduling and audit.') }}</p></div></div>
      </div>
    </section>

    <section class="workbench-section" aria-labelledby="workbench-title"><div class="page-width">
      <div class="section-heading"><div><h2 id="workbench-title">{{ t('把业务意图，写进一个目录。', 'Give the job a directory.') }}</h2><p>{{ t('三个目标验收场景，共用同一个运行底座。选择任务，看看定义如何变化。', 'Three planned acceptance scenarios, one shared runtime. Choose a job to inspect its definition.') }}</p></div><span class="design-note">{{ t('目标配置示例 · 尚不可执行', 'Target configuration · Not executable yet') }}</span></div>
      <div class="example-tabs" role="tablist" :aria-label="t('Agent 目标场景', 'Planned Agent scenarios')"><button v-for="(example, index) in examples" :id="`tab-${example.id}`" :key="example.id" type="button" role="tab" :aria-selected="selectedId === example.id" aria-controls="agent-example-panel" :tabindex="selectedId === example.id ? 0 : -1" @click="selectedId = example.id" @keydown="selectWithKeyboard($event, index)">{{ example.tab }}</button></div>
      <div id="agent-example-panel" class="agent-workbench" role="tabpanel" :aria-labelledby="`tab-${activeExample.id}`" tabindex="0">
        <div class="agent-overview">
          <div class="folder-symbol" aria-hidden="true"><svg viewBox="0 0 56 46"><path d="M4 12V7a3 3 0 0 1 3-3h15l6 7h21a3 3 0 0 1 3 3v25a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V12Z" /><path d="M4 15h48" /></svg></div>
          <h3>{{ activeExample.title }}</h3><p>{{ activeExample.description }}</p>
          <div class="directory-tree" :aria-label="t('目标目录结构', 'Target directory structure')"><div class="tree-root">.oryxos/agents/</div><div class="tree-agent">└── {{ activeExample.id }}/</div><div class="tree-file">&nbsp;&nbsp;&nbsp;&nbsp;{{ activeExample.binding ? '├──' : '└──' }} AGENT.md</div><pre v-if="activeExample.binding">    └── {{ activeExample.binding }}</pre></div>
          <p class="example-note">{{ activeExample.note }}</p>
        </div>
        <div class="agent-file"><div class="file-header"><span><span class="file-icon" aria-hidden="true" />AGENT.md</span><span>{{ t('运行配置 + 任务指令', 'Configuration + instructions') }}</span></div><pre tabindex="0" :aria-label="t('Agent 定义示例', 'Example Agent definition')"><code>{{ agentDefinition }}</code></pre></div>
      </div>
    </div></section>

    <section class="runtime-section page-width" aria-labelledby="runtime-title">
      <div class="runtime-intro"><h2 id="runtime-title">{{ t('共享底座。\n明确边界。', 'Shared runtime.\nClear boundaries.') }}</h2><p>{{ t('一份 Agent 定义背后，是可检查、可扩展的运行链路。Java 的工程体系不换，Agent 的执行机制由自己掌控。', 'Behind each Agent definition is an inspectable, extensible execution path. Keep the Java engineering ecosystem; own the Agent loop.') }}</p><a class="text-link" :href="link('/docs/architecture')">{{ t('阅读技术架构', 'Read the architecture') }}</a></div>
      <div class="capability-list"><div class="capability-caption"><span>{{ t('Runtime MVP 的五块能力', 'Five parts of the Runtime MVP') }}</span><span>{{ t('均在开发中', 'All in development') }}</span></div><article v-for="capability in capabilities" :key="capability.contract" class="capability-row"><h3>{{ capability.name }}</h3><div><p>{{ capability.detail }}</p><code>{{ capability.contract }}</code></div></article></div>
    </section>

    <section class="architecture-section page-width" aria-labelledby="architecture-title">
      <div class="section-heading"><div><h2 id="architecture-title">{{ t('从一条消息，到一次可追溯的执行。', 'From a message to a traceable run.') }}</h2><p>{{ t('目标逻辑架构：三个触发入口，一条执行链路。图中业务组件尚未实现。', 'Target logical architecture: three triggers, one execution path. Business components shown here are not implemented yet.') }}</p></div><a class="text-link" :href="withBase('/images/architecture.svg')" target="_blank" rel="noopener noreferrer">{{ t('查看完整架构图', 'Open full diagram') }}</a></div>
      <figure class="architecture-figure"><div class="architecture-canvas" tabindex="0" :aria-label="t('架构图；窄屏可横向滚动', 'Architecture diagram; scroll horizontally on narrow screens')"><img :src="withBase('/images/architecture.svg')" :alt="t('OryxOS 目标逻辑架构：CLI、REST、定时任务统一进入 AgentService；ReAct 联合 Provider、Memory 与 Tool 执行，并将调用审计写入 SQLite。', 'OryxOS target architecture: CLI, REST and schedules enter AgentService. ReAct uses Provider, Memory and Tool capabilities; calls are audited in SQLite.')" width="1440" height="1040" loading="lazy" /></div><figcaption><span class="mobile-diagram-hint">{{ t('横向滑动查看，或打开完整图。', 'Scroll sideways or open the full diagram.') }}</span>{{ t('逻辑调用关系，不是 Maven 依赖图。core 定义接口，能力模块实现接口，boot 聚合。', 'Logical call flow, not Maven dependencies. Core defines contracts; capability modules implement them; boot assembles them.') }}</figcaption></figure>
      <div class="boundary-note"><span class="boundary-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="m12 3 8 3v6c0 4-5 7-8 9-3-2-8-5-8-9V6l8-3Z" fill="none" stroke="currentColor" stroke-width="1.5" /><path d="M12 8v5m0 3v.5" stroke="currentColor" stroke-width="1.5" /></svg></span><p><strong>{{ t('私有部署不是一句“数据绝不出域”。', 'Self-hosting is not a promise of zero data egress.') }}</strong>{{ t('工作区与存储由你管理；使用云端模型时，请求会发送到配置的 Provider。核心阶段的工具白名单也不等于可运行不可信代码的强隔离沙箱。', 'You control the workspace and storage. Cloud-model requests still go to the configured Provider. Core-stage tool allow-lists are not strong isolation for untrusted code.') }}</p></div>
    </section>

    <section class="progress-section" aria-labelledby="progress-title"><div class="page-width">
      <div class="progress-heading"><h2 id="progress-title">{{ t('愿景很大，\n先把内核做实。', 'A broad vision.\nA concrete first step.') }}</h2><p>{{ t('当前聚焦单机 Runtime MVP。多租户、团队编排和分布式协作不属于核心阶段。', 'Start with a single-node Runtime MVP. Multi-tenancy, team orchestration and distributed collaboration are outside the core stage.') }}</p></div>
      <ol class="progress-track"><li class="complete"><span class="progress-status">{{ t('已实现', 'Implemented') }}</span><h3>{{ t('工程骨架', 'Engineering skeleton') }}</h3><p>{{ t('9 个 Maven 模块、Boot 启动入口、CLI 版本入口与双语网站。', 'Nine Maven modules, a Boot entry, a CLI version entry and a bilingual website.') }}</p></li><li class="current"><span class="progress-status">{{ t('当前开发目标', 'Current development target') }}</span><h3>Runtime MVP</h3><p>{{ t('Provider → ReAct → Memory / Tool → REST、持久化与 CLI。核心能力尚未实现。', 'Provider → ReAct → Memory / Tool → REST, persistence and CLI. Core capabilities are not implemented yet.') }}</p></li><li><span class="progress-status">{{ t('后续规划', 'Later roadmap') }}</span><h3>Agent OS</h3><p>{{ t('在可运行内核之上，逐步补齐生命周期、治理与企业级扩展。', 'Build lifecycle management, governance and enterprise extensions on a working kernel.') }}</p></li></ol>
      <div class="progress-links"><a :href="`${REPO_URL}/blob/main/docs/IMPLEMENTATION_STATUS.md`" target="_blank" rel="noopener noreferrer">{{ t('核对真实实现状态', 'Check implementation status') }}</a><a :href="link('/docs/roadmap')">{{ t('查看路线图', 'View the roadmap') }}</a></div>
    </div></section>

    <section class="closing-section page-width" aria-labelledby="closing-title"><div><img :src="withBase('/images/logo-mark.svg')" alt="" width="44" height="44" loading="lazy" /><h2 id="closing-title">{{ t('给 Agent 一个长期工作的地方。', 'Give your Agents a place to keep working.') }}</h2><p>{{ t('从理解设计开始，也欢迎一起把运行时做出来。', 'Start with the design. Help us build the runtime.') }}</p></div><div class="closing-actions"><a class="button button-primary" :href="link('/docs/quick-start')">{{ t('构建当前工程', 'Build the project') }}</a><a class="text-link" :href="REPO_URL" target="_blank" rel="noopener noreferrer">{{ t('在 GitHub 参与', 'Join on GitHub') }}</a></div></section>
    <footer class="oryx-footer page-width"><span>OryxOS</span><span>{{ t('基于 Apache 2.0 协议开源', 'Released under the Apache 2.0 License') }}</span></footer>
  </div>
</template>

<style scoped>
.oryx-home { --home-cloud: #f4f5f2; --home-ink: #292536; --home-violet: #6650b8; --home-lilac: #e4dff1; --home-mint: #d2e5dc; --home-paper: #fff; color: var(--home-ink); background: var(--home-cloud); }
.page-width { width: min(1200px, calc(100% - 96px)); margin-inline: auto; }
.hero { display: grid; grid-template-columns: 1fr 1fr; gap: 0 36px; padding-top: 68px; }
.stage-label { display: inline-flex; align-items: center; gap: 8px; padding: 6px 10px 6px 8px; border: 1px solid #d6d2de; border-radius: 6px; font-size: 12px; line-height: 20px; color: #575060; background: #fafaf8; }
.stage-label:hover { border-color: var(--home-violet); color: var(--home-violet); }
.stage-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--home-violet); flex-shrink: 0; }
h1 { margin: 27px 0 24px; font-size: clamp(52px, 5.45vw, 76px); font-weight: 600; line-height: 1.17; letter-spacing: -0.065em; }
.is-en h1 { font-size: clamp(50px, 5.2vw, 72px); line-height: 1.08; letter-spacing: -0.055em; }
.hero-description { max-width: 460px; margin: 0; font-size: 16px; line-height: 1.85; color: #625d6b; }
.hero-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-top: 28px; }
.button { display: inline-flex; justify-content: center; align-items: center; gap: 9px; min-height: 48px; padding: 11px 22px; border-radius: 8px; border: 1px solid transparent; font-size: 14px; font-weight: 600; line-height: 24px; transition: background .18s, border-color .18s; }
.button-primary { background: var(--home-ink); color: white; }
.button-primary:hover { background: #484055; }
.button-secondary { border-color: #ccc8d3; background: transparent; }
.button-secondary:hover { border-color: var(--home-ink); background: #fff; }
.hero-honesty { max-width: 440px; margin: 20px 0 0; font-size: 12px; line-height: 1.8; color: #706b78; }
.hero-figure { margin: 46px -24px 0 -16px; align-self: start; }
.hero-figure figcaption { display: flex; justify-content: space-between; gap: 12px; margin: 0 33px; padding-top: 12px; border-top: 1px solid #d5d1dc; font-size: 12px; color: #706b78; }
.hero-figure figcaption span:first-child { color: #494152; }
.hero-foundation { grid-column: 1 / -1; display: flex; justify-content: space-between; gap: 20px; margin-top: 58px; padding: 24px 0; border-top: 1px solid #d8d5de; border-bottom: 1px solid #d8d5de; }
.hero-foundation span { display: flex; align-items: baseline; gap: 16px; font-size: 12px; color: #706b78; }
.hero-foundation b { font-size: 15px; font-weight: 600; color: var(--home-ink); }
h2 { font-size: clamp(28px, 3vw, 40px); font-weight: 600; line-height: 1.35; letter-spacing: -.045em; margin: 0; white-space: pre-line; }
p { margin: 0; }
.idea-section { display: grid; grid-template-columns: 5fr 7fr; gap: 60px; padding-block: 98px; }
.section-mark { display: block; width: 32px; height: 32px; color: var(--home-violet); margin-bottom: 22px; }
.section-mark svg { width: 100%; height: 100%; }
.idea-copy > .lead { white-space: pre-line; font-size: 24px; line-height: 1.5; letter-spacing: -.025em; margin-bottom: 24px; }
.idea-copy > p:not(.lead) { max-width: 590px; font-size: 15px; line-height: 1.9; color: #625d6b; }
.responsibility-split { display: grid; grid-template-columns: 1fr 1fr; margin-top: 32px; border-top: 1px solid #d8d5de; }
.responsibility-split > div { padding: 20px 20px 0 0; }
.responsibility-split > div + div { padding-left: 24px; border-left: 1px solid #d8d5de; }
.responsibility-split span { font-size: 13px; font-weight: 600; }
.responsibility-split p { margin-top: 9px; font-size: 13px; line-height: 1.8; color: #625d6b; }
.workbench-section { background: var(--home-lilac); padding: 66px 0 74px; }
.section-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 32px; }
.section-heading h2 { font-size: 30px; }
.section-heading p { max-width: 720px; margin-top: 14px; font-size: 14px; line-height: 1.8; color: #625d6b; }
.design-note { flex-shrink: 0; margin-top: 13px; color: #655b76; font-size: 12px; }
.example-tabs { display: flex; flex-wrap: wrap; gap: 6px; margin: 32px 0 16px; }
.example-tabs button { padding: 10px 20px; border-radius: 7px; font-size: 14px; line-height: 24px; color: #5f566e; text-align: left; }
.example-tabs button:hover { background: #ffffff66; }
.example-tabs button[aria-selected='true'] { background: var(--home-ink); color: white; }
.agent-workbench { display: grid; grid-template-columns: 5fr 7fr; overflow: hidden; border: 1px solid #c6bcd8; border-radius: 14px; background: #f6f4fa; }
.agent-overview { padding: 36px; min-width: 0; }
.folder-symbol { width: 56px; height: 46px; margin-bottom: 24px; }
.folder-symbol svg { fill: var(--home-mint); stroke: #6f877b; stroke-width: 1.2; }
.agent-overview h3 { font-size: 22px; line-height: 1.5; font-weight: 600; letter-spacing: -.035em; margin: 0 0 14px; }
.agent-overview > p:not(.example-note) { font-size: 14px; line-height: 1.9; color: #625d6b; }
.directory-tree { overflow-x: auto; margin-top: 28px; padding: 20px; border: 1px solid #dcd7e7; border-radius: 8px; background: #ece9f3; font-family: var(--vp-font-family-mono); font-size: 12px; line-height: 1.9; white-space: nowrap; }
.directory-tree pre { margin: 0; font: inherit; }
.tree-root { color: #6c6278; }
.tree-file { color: #554191; }
.example-note { margin-top: 24px; font-size: 12px; line-height: 1.8; color: #706679; }
.agent-file { min-width: 0; background: var(--home-paper); border-left: 1px solid #d4cbe1; }
.file-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; padding: 17px 24px; background: #faf9fc; border-bottom: 1px solid #e4dfec; font-size: 12px; color: #766d84; }
.file-header > span:first-child { display: flex; align-items: center; gap: 9px; font-weight: 600; color: #4c405e; }
.file-icon { width: 12px; height: 15px; border: 1px solid #9589aa; border-radius: 2px; }
.agent-file pre { margin: 0; padding: 26px 24px 30px; font-size: 12px; line-height: 1.9; overflow-x: auto; scrollbar-width: thin; }
.agent-file code { font-family: var(--vp-font-family-mono); color: #55476b; }
.runtime-section { display: grid; grid-template-columns: 4fr 8fr; gap: 72px; padding-block: 96px 84px; }
.runtime-intro p { margin-top: 24px; color: #625d6b; font-size: 14px; line-height: 1.85; }
.runtime-intro .text-link { display: inline-block; margin-top: 24px; }
.text-link { color: var(--home-violet); font-size: 13px; font-weight: 600; text-decoration: underline; text-underline-offset: 5px; text-decoration-color: #bfb4d8; }
.text-link:hover { color: #433378; text-decoration-color: currentColor; }
.capability-caption { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 15px; border-bottom: 1px solid #bcb6c7; font-size: 12px; color: #736b7d; }
.capability-caption span:last-child { color: var(--home-violet); }
.capability-row { display: grid; grid-template-columns: 150px 1fr; gap: 18px; padding: 22px 0; border-bottom: 1px solid #d8d5de; }
.capability-row h3 { margin: 0; font-size: 16px; line-height: 1.7; font-weight: 600; }
.capability-row p { color: #625d6b; font-size: 14px; line-height: 1.8; }
.capability-row code { display: inline-block; margin-top: 7px; color: #6b6279; font-family: var(--vp-font-family-mono); font-size: 11px; }
.architecture-section { padding-bottom: 88px; }
.architecture-section .section-heading .text-link { flex-shrink: 0; margin-top: 12px; }
.architecture-figure { margin: 28px 0 0; background: white; border: 1px solid #d8d5de; border-radius: 12px; overflow: hidden; }
.architecture-canvas { overflow-x: auto; scrollbar-width: thin; }
.mobile-diagram-hint { display: none; }
.architecture-figure img { display: block; width: 100%; height: auto; }
.architecture-figure figcaption { border-top: 1px solid #e6e2ec; padding: 17px 24px; font-size: 12px; line-height: 1.7; color: #716879; background: #faf9fc; }
.boundary-note { display: flex; gap: 15px; margin-top: 24px; }
.boundary-icon { flex: 0 0 24px; color: #817092; margin-top: 3px; }
.boundary-icon svg { width: 24px; height: 24px; }
.boundary-note p { font-size: 12px; line-height: 1.9; color: #746c7d; }
.boundary-note strong { font-weight: 600; color: #4c415d; margin-right: 5px; }
.progress-section { padding-block: 68px; background: var(--home-mint); }
.progress-heading { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; align-items: end; }
.progress-heading p { max-width: 440px; justify-self: end; color: #4d6355; font-size: 14px; line-height: 1.85; }
.progress-track { list-style: none; display: grid; grid-template-columns: repeat(3, 1fr); gap: 40px; padding: 0; margin: 40px 0 28px; }
.progress-track li { padding: 20px 0 0; border-top: 2px solid #afc1b6; position: relative; }
.progress-track li::before { content: ''; position: absolute; top: -6px; left: 0; width: 10px; height: 10px; border: 2px solid #97afa0; background: var(--home-mint); border-radius: 50%; }
.progress-track .complete { border-color: #6f897a; }
.progress-track .complete::before { background: #6f897a; border-color: #6f897a; }
.progress-track .current { border-color: var(--home-ink); }
.progress-track .current::before { background: var(--home-ink); border-color: var(--home-ink); }
.progress-status { font-size: 12px; color: #4d6355; }
.progress-track h3 { margin: 12px 0; font-size: 22px; font-weight: 600; letter-spacing: -.025em; }
.progress-track p { max-width: 330px; font-size: 13px; line-height: 1.85; color: #4d6355; }
.progress-links { display: flex; flex-wrap: wrap; gap: 24px; font-size: 13px; font-weight: 600; }
.progress-links a { text-decoration: underline; text-decoration-color: #8ca798; text-underline-offset: 5px; }
.progress-links a:hover { text-decoration-color: var(--home-ink); }
.closing-section { display: flex; justify-content: space-between; align-items: center; gap: 40px; padding-block: 76px; }
.closing-section img { display: block; margin-bottom: 24px; }
.closing-section h2 { font-size: 28px; }
.closing-section p { margin-top: 12px; font-size: 14px; color: #726b7c; }
.closing-actions { display: flex; flex-direction: column; align-items: center; gap: 18px; flex-shrink: 0; }
.oryx-footer { display: flex; justify-content: space-between; gap: 24px; padding: 24px 0 30px; border-top: 1px solid #d8d5de; font-size: 12px; color: #6f6777; }
.oryx-footer > span:first-child { font-weight: 600; color: #51465f; }
.oryx-home :is(a, button, [tabindex]):focus-visible { outline: 3px solid var(--home-violet); outline-offset: 5px; border-radius: 5px; }
@media (min-width: 1440px) { .hero { padding-top: 80px; } }
@media (max-width: 1100px) {
  .page-width { width: calc(100% - 64px); }
  .hero { gap: 0 16px; padding-top: 54px; }
  .hero-figure { margin: 58px -16px 0; }
  h1 { font-size: 59px; }
  .is-en h1 { font-size: 54px; }
  .hero-foundation span { flex-direction: column; gap: 4px; }
  .idea-section { gap: 40px; }
  .section-heading { flex-direction: column; gap: 10px; }
  .design-note { margin-top: 0; }
  .agent-overview { padding: 28px; }
  .runtime-section { gap: 40px; grid-template-columns: 4fr 7fr; }
  .capability-row { grid-template-columns: 120px 1fr; }
  .closing-section h2 { font-size: 24px; }
}
@media (max-width: 760px) {
  .page-width { width: calc(100% - 40px); }
  .hero { grid-template-columns: 1fr; padding-top: 32px; }
  h1 { font-size: clamp(40px, 12.5vw, 62px); margin-top: 25px; }
  .is-en h1 { font-size: clamp(38px, 12vw, 60px); }
  .hero-description { max-width: 550px; font-size: 15px; }
  .hero-honesty { max-width: 520px; }
  .hero-figure { margin: 26px auto 0; width: min(100%, 560px); }
  .hero-figure figcaption { margin-inline: 15px; }
  .hero-foundation { margin-top: 28px; gap: 12px; padding-block: 20px; }
  .hero-foundation b { font-size: 12px; }
  .hero-foundation span { font-size: 11px; }
  .idea-section { grid-template-columns: 1fr; gap: 30px; padding-block: 60px; }
  .section-mark { margin-bottom: 15px; }
  .idea-copy > .lead { font-size: 21px; }
  .workbench-section { padding-block: 46px; }
  .section-heading h2 { font-size: 26px; }
  .example-tabs { gap: 4px; margin-top: 24px; }
  .example-tabs button { font-size: 13px; padding: 9px 14px; }
  .agent-workbench { grid-template-columns: 1fr; border-radius: 10px; }
  .agent-overview { padding: 26px; }
  .folder-symbol { margin-bottom: 16px; width: 46px; height: 38px; }
  .agent-overview h3 { font-size: 21px; }
  .directory-tree { margin-top: 20px; }
  .example-note { margin-top: 18px; }
  .agent-file { border-left: 0; border-top: 1px solid #d4cbe1; }
  .file-header { padding-inline: 20px; font-size: 11px; }
  .agent-file pre { padding: 22px 20px; font-size: 11px; }
  .runtime-section { grid-template-columns: 1fr; gap: 36px; padding-block: 60px; }
  .runtime-intro p { max-width: 520px; margin-top: 18px; }
  .capability-row { grid-template-columns: 108px 1fr; gap: 12px; padding-block: 18px; }
  .capability-row h3 { font-size: 14px; }
  .capability-row p { font-size: 13px; }
  .architecture-section { padding-bottom: 56px; }
  .architecture-figure { margin-top: 20px; }
  .architecture-canvas img { min-width: 1080px; }
  .mobile-diagram-hint { display: block; margin-bottom: 5px; color: var(--home-violet); }
  .architecture-figure figcaption { padding: 14px 16px; font-size: 11px; }
  .boundary-note { gap: 11px; }
  .progress-section { padding-block: 46px; }
  .progress-heading { grid-template-columns: 1fr; gap: 22px; }
  .progress-heading p { justify-self: start; max-width: 520px; }
  .progress-track { grid-template-columns: 1fr; gap: 0; margin-top: 32px; }
  .progress-track li { padding: 0 0 28px 25px; border-top: 0; border-left: 2px solid #afc1b6; }
  .progress-track li:last-child { padding-bottom: 0; border-left-color: transparent; }
  .progress-track li::before { left: -6px; top: 5px; }
  .progress-track h3 { font-size: 20px; margin-block: 7px; }
  .progress-track p { max-width: 100%; }
  .progress-links { gap: 18px; font-size: 12px; }
  .closing-section { flex-direction: column; align-items: flex-start; gap: 26px; padding-block: 48px; }
  .closing-section h2 { font-size: 25px; }
  .closing-section p { line-height: 1.8; }
  .closing-actions { flex-direction: row; flex-wrap: wrap; gap: 20px; }
  .closing-actions .button { padding-inline: 18px; }
  .oryx-footer { padding-block: 20px; font-size: 11px; gap: 12px; }
}
@media (prefers-reduced-motion: reduce) { .button { transition: none; } }
</style>
