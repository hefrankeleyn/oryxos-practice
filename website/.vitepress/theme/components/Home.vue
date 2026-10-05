<script setup>
import { computed } from 'vue'
import { useData, withBase } from 'vitepress'

const REPO_URL = 'https://github.com/hefrankeleyn/oryxos-practice'

const { lang } = useData()
const isZh = computed(() => lang.value === 'zh-CN')
/** 双语文案：中文站（根路径）取 zh，英文站（/en/）取 en。 */
const t = (zh, en) => (isZh.value ? zh : en)
/** 按当前语言拼站内链接。 */
const link = (path) => withBase(isZh.value ? path : `/en${path}`)

const capabilities = computed(() => [
  {
    icon: '🤖',
    title: t('一个目录 = 一个 Agent', 'One directory = one Agent'),
    subtitle: t('AGENT.md · frontmatter 即配置 · 正文即指令', 'AGENT.md · frontmatter is config · body is instructions'),
    code: `# .oryxos/agents/weather/AGENT.md
---
name: weather
provider: { name: deepseek, model: deepseek-chat }
tools: [http_get, notify]
schedules:
  - key: morning
    cron: "0 0 8 * * *"
    zone: Asia/Shanghai
    message: ${t('查询北京天气并给出穿搭建议', 'Check the weather in Beijing')}
---

${t('1. 调用天气 API 获取今天的天气；\n2. 结合温度、降水、风力给出穿搭建议；\n3. 通过 notify 推送到 team-lark 渠道。', '1. Call the weather API for today\'s weather;\n2. Suggest what to wear from temperature, rain and wind;\n3. Push it to the team-lark channel via notify.')}`,
  },
  {
    icon: '🔌',
    title: t('零代码接入 MCP 工具', 'Zero-code MCP tools'),
    subtitle: t('复用社区 MCP server · 凭证走环境变量', 'Reuse community MCP servers · secrets via env'),
    code: `# .oryxos/mcp_servers.yaml
servers:
  - name: github
    transport: stdio
    command: npx -y @modelcontextprotocol/server-github
    env:
      GITHUB_TOKEN: \${GITHUB_TOKEN}

${t('# 启动时自动连接，tools/list 拉取工具，\n# 包装成 OryxTool 注册，Agent 直接可用；\n# 每次调用都过沙箱、写审计。', '# Connected at startup; tools/list fetches tools,\n# wrapped as OryxTool and ready for Agents;\n# every call is sandboxed and audited.')}`,
  },
  {
    icon: '🌐',
    title: t('REST API 接入业务系统', 'REST API for your systems'),
    subtitle: t('任何语言 · 会话保持 · 无状态调用', 'Any language · sessions · stateless invoke'),
    code: `${t('# 无状态调用一次 Agent', '# One-shot, stateless invocation')}
curl -X POST localhost:8080/api/v1/agents/weather/invoke \\
  -H 'Content-Type: application/json' \\
  -d '{"message":"${t('明天上海要带伞吗？', 'Umbrella in Shanghai tomorrow?')}"}'

${t('# 会话保持：创建会话，多轮对话', '# Sessions: create one, then chat')}
curl -X POST localhost:8080/api/v1/sessions \\
  -d '{"profile":"weather","userId":"u-001"}'
curl -X POST localhost:8080/api/v1/sessions/{id}/messages \\
  -d '{"message":"${t('那后天呢？', 'And the day after?')}"}'`,
  },
])

const scenarios = computed(() => [
  {
    num: '01',
    title: t('每日天气推送', 'Daily weather digest'),
    desc: t('每天早上 8 点自动查天气、生成穿搭建议并推送到企业 IM 群。只有一份 AGENT.md，不需要人工触发。', 'Every morning at 8, fetch the weather, suggest what to wear and push it to the team IM group. A single AGENT.md, no manual trigger.'),
  },
  {
    num: '02',
    title: t('每日科技日报', 'Daily tech digest'),
    desc: t('到点汇总当日科技新闻，按公共 Skill 规定的格式组稿；因为记得你说过“更关注 AI 和芯片”，日报会自然侧重这两个方向。', 'Summarize today\'s tech news in a format defined by a shared Skill. Because it remembers you care about AI and chips, the digest leans that way.'),
  },
  {
    num: '03',
    title: t('每日 GitHub 日报', 'Daily GitHub trending'),
    desc: t('Agent 目录捆绑一个脚本，拿到确定性的 trending 数据，再挑出 AI 相关项目做总结推送。脚本输出进上下文，脚本代码不进。', 'An Agent directory bundles a script for deterministic trending data, then summarizes the AI-related projects. Script output enters the context; script code does not.'),
  },
  {
    num: '04',
    title: t('运维助手', 'Ops assistant'),
    desc: t('告警经 Webhook 进来，Agent 拉日志、比对历史故障、按 runbook 自愈，并在运维群汇报。所有动作都有审计记录。', 'Alerts arrive via webhook; the Agent pulls logs, matches past incidents, self-heals per runbook and reports to the ops group — every action audited.'),
  },
  {
    num: '05',
    title: t('全渠道客服', 'Omni-channel support'),
    desc: t('理解用户问题、循环检索知识库、记住客户历史、查询 CRM，通过 HTTP 接入现有客服系统。', 'Understands questions, searches the knowledge base, remembers customer history, queries the CRM — plugged into your support system over HTTP.'),
  },
  {
    num: '06',
    title: t('研发助手', 'Engineering assistant'),
    desc: t('理解需求、读代码改代码、记住项目惯例，接 GitHub 与 CI，从 IDE 插件或内部平台调用。', 'Understands requirements, reads and edits code, remembers project conventions, connects to GitHub and CI — called from IDE plugins or internal portals.'),
  },
  {
    num: '07',
    title: t('知识管理助手', 'Knowledge assistant'),
    desc: t('检索合同模板、法规文档和历史案例，给出带引用来源的建议草稿，满足“回复必须可追溯”的合规要求。', 'Searches contract templates, regulations and past cases, drafting advice with citations — meeting the compliance rule that every answer is traceable.'),
  },
  {
    num: '08',
    title: t('数据分析助手', 'Data analysis assistant'),
    desc: t('记住业务表结构，生成 SQL、执行查询并出图，集成到企业现有的 BI 工具中。', 'Remembers your schemas, writes SQL, runs queries and charts the result — integrated into your existing BI tools.'),
  },
])
</script>

<template>
  <div class="oryx-page">

    <!-- ── HERO ── -->
    <section class="oryx-hero">
      <div class="oryx-hero-glow"></div>
      <div class="oryx-hero-inner">
        <div class="oryx-badge">
          <span class="oryx-badge-dot"></span>
          {{ t('Agent Harness OS · 开源 · Apache 2.0', 'Agent Harness OS · Open Source · Apache 2.0') }}
        </div>

        <img class="oryx-hero-logo" :src="withBase('/images/logo-mark.svg')" alt="OryxOS logo" />

        <h1 class="oryx-title">
          <span class="oryx-title-name">Oryx<span class="oryx-title-os">OS</span></span>
        </h1>

        <p class="oryx-title-sub">{{ t('Java 原生的企业级 Agent 操作系统', 'The Java-native Agent OS for the enterprise') }}</p>

        <p class="oryx-hero-desc">
          {{ t('一个目录定义一个 Agent，一个底座运行一群 Agent。OryxOS 装在企业自己的服务器上，为所有业务 Agent 提供统一的模型接入、推理循环、记忆、工具调用、沙箱与审计——私有部署，数据不出域。', 'One directory defines an Agent; one runtime hosts them all. OryxOS runs on your own servers and gives every business Agent shared model access, a reasoning loop, memory, tool calling, sandboxing and audit — self-hosted, data never leaves.') }}
        </p>

        <div class="oryx-hero-actions">
          <a class="oryx-btn-primary" :href="link('/docs/quick-start')">
            {{ t('开始使用', 'Get Started') }} →
          </a>
          <a class="oryx-btn-ghost" :href="link('/docs/architecture')">
            {{ t('系统架构', 'Architecture') }}
          </a>
          <a class="oryx-btn-ghost" :href="REPO_URL" target="_blank" rel="noopener">
            GitHub
          </a>
        </div>

        <div class="oryx-hero-note">
          JDK 21 · Spring Boot 3 · Spring AI Alibaba · MCP · SQLite · {{ t('单 JAR 部署', 'Single JAR') }}
        </div>
      </div>
    </section>

    <!-- ── PROBLEM ── -->
    <section class="oryx-section">
      <div class="oryx-section-inner">
        <div class="oryx-problem">
          <div class="oryx-problem-text">
            <h2 class="oryx-section-title">{{ t('Agent 为什么停在 demo', 'Why Agents stall at the demo') }}</h2>
            <p>{{ t('每家公司都有该交给 Agent 的活，但真正上生产的很少。让 Agent 可靠工作，瓶颈通常不在模型，而在运行环境。', 'Every company has work for Agents, yet few reach production. The bottleneck is rarely the model — it is the environment Agents run in.') }}</p>
            <p class="oryx-problem-item">
              <strong>{{ t('① 定义一个 Agent 要写代码', '① Defining an Agent means writing code') }}</strong>
              {{ t('最懂业务的人反而做不了。', 'The people who know the business can\'t do it.') }}
            </p>
            <p class="oryx-problem-item">
              <strong>{{ t('② 云平台要把数据拿走', '② Cloud platforms take your data') }}</strong>
              {{ t('严监管行业的合规过不去。', 'Regulated industries can\'t pass compliance.') }}
            </p>
            <p class="oryx-problem-item">
              <strong>{{ t('③ 执行是黑盒', '③ Execution is a black box') }}</strong>
              {{ t('没审计、没白名单，企业不敢上生产。', 'No audit, no allow-lists — nobody dares ship it.') }}
            </p>
            <p class="oryx-problem-item">
              <strong>{{ t('④ 跑一个容易，跑一群难', '④ One Agent is easy, a fleet is hard') }}</strong>
              {{ t('没有人把“一群 Agent 的操作系统”这一层交给你。', 'Nobody hands you the operating-system layer for a fleet of Agents.') }}
            </p>
            <p class="oryx-solution-line">{{ t('OryxOS 一次拆掉这四道门槛。', 'OryxOS removes all four barriers at once.') }}</p>
          </div>
          <div class="oryx-problem-compare">
            <div class="oryx-compare-item oryx-compare-bad">
              <div class="oryx-compare-label">{{ t('今天的做法', 'Today') }}</div>
              <div class="oryx-compare-rows">
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon">✗</span>
                  <span>{{ t('每个 Agent 都写一套后端代码', 'A new backend for every Agent') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon">✗</span>
                  <span>{{ t('SaaS 平台，数据出域、锁定生态', 'SaaS platforms — data leaves, lock-in follows') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon">✗</span>
                  <span>{{ t('工具调用不设防，事后翻日志补审计', 'Unguarded tool calls, audit rebuilt from logs') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon">✗</span>
                  <span>{{ t('Node.js / Python 方案，与 Java 体系之间全是胶水', 'Node.js / Python stacks glued to Java systems') }}</span>
                </div>
              </div>
            </div>
            <div class="oryx-compare-item oryx-compare-good">
              <div class="oryx-compare-label">OryxOS</div>
              <div class="oryx-compare-rows">
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon oryx-icon-ok">✓</span>
                  <span>{{ t('写一份 AGENT.md，零代码定义 Agent', 'Write one AGENT.md — zero-code Agents') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon oryx-icon-ok">✓</span>
                  <span>{{ t('装在自己的 K8s / 虚拟机上，数据不出域', 'Runs on your K8s or VMs — data stays in-house') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon oryx-icon-ok">✓</span>
                  <span>{{ t('强制沙箱白名单，每次调用落库审计', 'Mandatory sandbox, every call audited in the DB') }}</span>
                </div>
                <div class="oryx-compare-row">
                  <span class="oryx-compare-icon oryx-icon-ok">✓</span>
                  <span>{{ t('Spring Boot 单 JAR，复用现有 Java 运维体系', 'One Spring Boot JAR on your Java toolchain') }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── ARCHITECTURE DIAGRAM ── -->
    <section class="oryx-section oryx-flow-section">
      <div class="oryx-section-inner">
        <img :src="withBase('/images/architecture.svg')" :alt="t('OryxOS 整体架构', 'OryxOS architecture')" class="oryx-flow-img" />
      </div>
    </section>

    <!-- ── CAPABILITIES ── -->
    <section class="oryx-section oryx-primitives-section">
      <div class="oryx-section-inner oryx-primitives-inner">
        <div class="oryx-section-header">
          <div class="oryx-section-tag">{{ t('核心能力', 'Core Capabilities') }}</div>
          <h2 class="oryx-section-title">{{ t('配置出 Agent，而不是写出 Agent', 'Configure Agents, don\'t code them') }}</h2>
          <p class="oryx-section-desc">{{ t('对接 LLM · ReAct 循环 · 记忆 · 工具体系 · Web Service，五大能力下沉到底座，业务方只写目录、配工具。', 'LLM access · ReAct loop · memory · tools · Web Service — five capabilities live in the runtime; you only write directories and wire tools.') }}</p>
        </div>
        <div class="oryx-primitives">
          <div v-for="p in capabilities" :key="p.title" class="oryx-primitive">
            <div class="oryx-primitive-header">
              <span class="oryx-primitive-icon">{{ p.icon }}</span>
              <div>
                <h3 class="oryx-primitive-title">{{ p.title }}</h3>
                <p class="oryx-primitive-subtitle">{{ p.subtitle }}</p>
              </div>
            </div>
            <pre class="oryx-code"><code>{{ p.code }}</code></pre>
          </div>
        </div>
      </div>
    </section>

    <!-- ── SCENARIOS ── -->
    <section class="oryx-section">
      <div class="oryx-section-inner">
        <div class="oryx-section-header">
          <div class="oryx-section-tag">{{ t('真实场景', 'Real Scenarios') }}</div>
          <h2 class="oryx-section-title">{{ t('八个典型使用场景', 'Eight typical use cases') }}</h2>
        </div>
        <div class="oryx-scenarios">
          <div v-for="s in scenarios" :key="s.num" class="oryx-scenario">
            <div class="oryx-scenario-num">{{ s.num }}</div>
            <div>
              <h3 class="oryx-scenario-title">{{ s.title }}</h3>
              <p class="oryx-scenario-desc">{{ s.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── TOOL TIERS ── -->
    <section class="oryx-section oryx-sdk-section">
      <div class="oryx-section-inner">
        <div class="oryx-section-header">
          <div class="oryx-section-tag">{{ t('工具扩展', 'Extending Tools') }}</div>
          <h2 class="oryx-section-title">{{ t('三档接入，门槛从低到高', 'Three tiers, from zero code to full Java') }}</h2>
          <p class="oryx-section-desc">{{ t('能用零代码就不用轻代码，能用轻代码就不用重代码。', 'Prefer zero code over light code, and light code over heavy code.') }}</p>
        </div>
        <div class="oryx-sdk-cards">
          <div class="oryx-sdk-card oryx-sdk-card-featured">
            <div class="oryx-sdk-card-icon">📝</div>
            <h3 class="oryx-sdk-card-title">{{ t('零代码 · 主推', 'Zero code · Recommended') }}</h3>
            <p class="oryx-sdk-card-desc">{{ t('写一个 Agent 目录，复用社区现成的 MCP server。业务方只描述意图，LLM 自己组合调用工具。', 'Write an Agent directory and reuse community MCP servers. Describe the intent; the LLM composes the tools.') }}</p>
            <div class="oryx-sdk-installs">
              <code>.oryxos/agents/&lt;name&gt;/AGENT.md</code>
              <code>.oryxos/mcp_servers.yaml</code>
            </div>
          </div>
          <div class="oryx-sdk-card">
            <div class="oryx-sdk-card-icon">🔌</div>
            <h3 class="oryx-sdk-card-title">{{ t('轻代码 · MCP server', 'Light code · MCP server') }}</h3>
            <p class="oryx-sdk-card-desc">{{ t('用任何语言写一个 MCP server，接入 ERP、CRM、CMDB 等企业自有系统，OryxOS 作为 MCP Client 连接。', 'Write an MCP server in any language to expose ERP, CRM or CMDB systems; OryxOS connects as the MCP client.') }}</p>
            <div class="oryx-langs">
              <span v-for="l in ['Java', 'Python', 'TypeScript', 'Go', 'Rust', 'Shell']" :key="l" class="oryx-lang">{{ l }}</span>
            </div>
          </div>
          <div class="oryx-sdk-card">
            <div class="oryx-sdk-card-icon">☕</div>
            <h3 class="oryx-sdk-card-title">{{ t('重代码 · @Tool Bean', 'Full code · @Tool Bean') }}</h3>
            <p class="oryx-sdk-card-desc">{{ t('用 Spring AI 的 @Tool 注解写 Java Bean，进程内直接调用企业现有 Java 服务，不走协议、不起进程，性能最好。', 'Annotate a Java Bean with Spring AI @Tool to call existing Java services in-process — no protocol hop, no extra process, best performance.') }}</p>
            <div class="oryx-sdk-badges">
              <span class="oryx-sdk-badge">Spring Boot</span>
              <span class="oryx-sdk-badge">Spring AI</span>
              <span class="oryx-sdk-badge">{{ t('进程内调用', 'In-process') }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── API & CLI ── -->
    <section class="oryx-section">
      <div class="oryx-section-inner">
        <div class="oryx-section-header">
          <div class="oryx-section-tag">{{ t('接口总览', 'Interfaces') }}</div>
          <h2 class="oryx-section-title">{{ t('REST API 与命令行', 'REST API & CLI') }}</h2>
          <p class="oryx-section-desc">{{ t('CLI、REST API、定时任务三个入口最终汇入同一个 AgentService，走同一条链路、同一套审计。', 'CLI, REST API and the scheduler all enter the same AgentService — one pipeline, one audit trail.') }}</p>
        </div>
        <div class="oryx-proto-grid">
          <div class="oryx-proto-group">
            <div class="oryx-proto-group-label">{{ t('会话管理', 'Sessions') }}</div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">POST /api/v1/sessions</code>
              <span class="oryx-proto-desc">{{ t('创建会话', 'Create a session') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">POST /api/v1/sessions/{id}/messages</code>
              <span class="oryx-proto-desc">{{ t('发送消息，驱动一轮 ReAct 循环', 'Send a message, run one ReAct loop') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">GET /api/v1/sessions/{id}</code>
              <span class="oryx-proto-desc">{{ t('查询会话历史（含工具调用链）', 'Session history, including tool calls') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">DELETE /api/v1/sessions/{id}</code>
              <span class="oryx-proto-desc">{{ t('归档会话', 'Archive a session') }}</span>
            </div>
          </div>
          <div class="oryx-proto-group">
            <div class="oryx-proto-group-label">{{ t('调用与查询', 'Invoke & Query') }}</div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">POST /api/v1/agents/{name}/invoke</code>
              <span class="oryx-proto-desc">{{ t('无状态调用一次 Agent', 'Stateless one-shot invocation') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">GET /api/v1/profiles</code>
              <span class="oryx-proto-desc">{{ t('列出已定义的 Agent', 'List defined Agents') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">GET /api/v1/memory</code>
              <span class="oryx-proto-desc">{{ t('查询长期记忆', 'Read long-term memory') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">GET /api/v1/tools</code>
              <span class="oryx-proto-desc">{{ t('列出可用工具', 'List available tools') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">GET /api/v1/health · /info</code>
              <span class="oryx-proto-desc">{{ t('健康检查与运行信息', 'Health check and runtime info') }}</span>
            </div>
          </div>
          <div class="oryx-proto-group">
            <div class="oryx-proto-group-label">{{ t('命令行', 'CLI') }}</div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">oryxos init · status</code>
              <span class="oryx-proto-desc">{{ t('初始化工作区（幂等）、查看运行状态', 'Init the workspace (idempotent), show status') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">oryxos chat · serve · gateway</code>
              <span class="oryx-proto-desc">{{ t('交互对话 / REST 服务 + 定时调度 / 多渠道守护进程', 'Interactive chat / REST + scheduler / multi-channel daemon') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">oryxos profile list|create|show|delete</code>
              <span class="oryx-proto-desc">{{ t('管理 Agent 目录', 'Manage Agent directories') }}</span>
            </div>
            <div class="oryx-proto-row">
              <code class="oryx-proto-subject">oryxos provider|tool|session list</code>
              <span class="oryx-proto-desc">{{ t('查看模型、工具与会话', 'Inspect providers, tools and sessions') }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── CTA ── -->
    <section class="oryx-section oryx-cta-section">
      <div class="oryx-section-inner">
        <div class="oryx-cta">
          <div class="oryx-wip">
            <span class="oryx-wip-dot"></span>{{ t('开发中', 'In development') }}
          </div>
          <h2 class="oryx-cta-title">{{ t('开始构建', 'Start Building') }}</h2>
          <p class="oryx-cta-desc">{{ t('OryxOS 正处于阶段一（单机运行时内核）开发中。以下是 1.0 的目标用法，接口在正式发布前可能调整。', 'OryxOS is in phase one (single-node runtime kernel). Below is the target 1.0 usage; interfaces may change before release.') }}</p>
          <pre class="oryx-code oryx-cta-code"><code>git clone https://github.com/hefrankeleyn/oryxos-practice.git
cd oryxos-practice && mvn clean package

export DEEPSEEK_API_KEY=sk-xxx

# {{ t('初始化工作区并创建一个 Agent', 'Initialize the workspace and create an Agent') }}
oryxos init
oryxos profile create weather

# {{ t('和 Agent 对话', 'Chat with it') }}
oryxos chat --profile weather

# {{ t('以服务方式运行（REST API + 定时调度）', 'Run as a service (REST API + scheduler)') }}
oryxos serve</code></pre>
          <div class="oryx-cta-links">
            <a class="oryx-btn-primary" :href="link('/docs/quick-start')">{{ t('查看文档', 'Read the Docs') }}</a>
            <a class="oryx-btn-ghost" :href="REPO_URL" target="_blank" rel="noopener">GitHub</a>
          </div>
        </div>
      </div>
    </section>

    <!-- ── FOOTER ── -->
    <footer class="oryx-footer">
      <span>{{ t('基于 Apache 2.0 协议开源', 'Released under the Apache 2.0 License') }}</span>
    </footer>

  </div>
</template>

<style scoped>
.oryx-page {
  min-height: 100vh;
  overflow-x: clip;
  background: #ffffff;
  color: #0f172a;
  font-family: inherit;
}

/* ── Hero ── */
.oryx-hero {
  position: relative;
  padding: 88px 24px 80px;
  text-align: center;
  overflow: hidden;
}
.oryx-hero-glow {
  position: absolute;
  top: -220px;
  left: 50%;
  width: 900px;
  height: 520px;
  transform: translateX(-50%);
  background: radial-gradient(ellipse at center, rgba(124, 58, 237, 0.14), rgba(79, 70, 229, 0.05) 45%, transparent 70%);
  pointer-events: none;
}
.oryx-hero-inner {
  position: relative;
  max-width: 780px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.oryx-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid var(--oryx-purple-line);
  background: var(--oryx-purple-soft);
  color: var(--oryx-purple-deep);
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 28px;
}
.oryx-badge-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--oryx-purple);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%,100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.4); }
}
.oryx-hero-logo {
  width: 84px;
  height: 84px;
  margin-bottom: 18px;
}
.oryx-title {
  margin: 0 0 14px;
  line-height: 1;
}
.oryx-title-name {
  font-size: clamp(64px, 12vw, 108px);
  font-weight: 900;
  letter-spacing: -0.04em;
  color: #0f172a;
}
.oryx-title-os {
  background: var(--oryx-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.oryx-title-sub {
  font-size: 19px;
  font-weight: 600;
  color: #475569;
  margin: 0 0 20px;
}
.oryx-hero-desc {
  font-size: 16px;
  line-height: 1.75;
  color: #475569;
  max-width: 640px;
  margin: 0 0 32px;
}
.oryx-hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  margin-bottom: 20px;
}
.oryx-btn-primary {
  padding: 11px 28px;
  border-radius: 8px;
  background: var(--oryx-gradient);
  color: #ffffff;
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  box-shadow: 0 6px 18px rgba(109, 40, 217, 0.25);
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
}
.oryx-btn-primary:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 10px 24px rgba(109, 40, 217, 0.3); }
.oryx-btn-ghost {
  padding: 11px 28px;
  border-radius: 8px;
  border: 1px solid #d4d4d8;
  background: #ffffff;
  color: #334155;
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
}
.oryx-btn-ghost:hover { border-color: var(--oryx-purple); background: var(--oryx-purple-soft); color: var(--oryx-purple-deep); }
.oryx-hero-note {
  font-size: 12px;
  color: #94a3b8;
}

/* ── Section ── */
.oryx-section { padding: 72px 24px; }
.oryx-section-inner { max-width: 1000px; margin: 0 auto; }
.oryx-primitives-inner { max-width: 1400px; }
.oryx-section-header { text-align: center; margin-bottom: 48px; }
.oryx-section-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--oryx-purple-deep);
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid var(--oryx-purple-line);
  background: var(--oryx-purple-soft);
  margin-bottom: 14px;
}
.oryx-section-title {
  font-size: clamp(22px, 4vw, 32px);
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px;
}
.oryx-section-desc {
  font-size: 15px;
  color: #64748b;
  max-width: 640px;
  margin: 0 auto;
  line-height: 1.6;
}

/* ── Problem ── */
.oryx-problem {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: start;
}
.oryx-problem-text p { color: #64748b; line-height: 1.7; margin: 0 0 14px; font-size: 15px; }
.oryx-problem-item strong { color: #0f172a; display: block; margin-bottom: 4px; }
.oryx-solution-line { color: var(--oryx-purple-deep) !important; font-weight: 700; }
.oryx-problem-compare { display: flex; flex-direction: column; gap: 16px; }
.oryx-compare-item {
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}
.oryx-compare-bad { background: #fafafa; }
.oryx-compare-good { background: var(--oryx-purple-soft); border-color: var(--oryx-purple-line); }
.oryx-compare-label { font-size: 11px; font-weight: 700; color: #94a3b8; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.08em; }
.oryx-compare-good .oryx-compare-label { color: var(--oryx-purple-deep); }
.oryx-compare-rows { display: flex; flex-direction: column; gap: 8px; }
.oryx-compare-row { display: flex; align-items: flex-start; gap: 10px; font-size: 13px; color: #475569; line-height: 1.5; }
.oryx-compare-icon { flex-shrink: 0; font-style: normal; color: #cbd5e1; font-weight: 700; width: 14px; }
.oryx-icon-ok { color: var(--oryx-purple); }

/* ── Capabilities ── */
.oryx-primitives-section { background: #f8f7fc; }
.oryx-primitives { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); grid-auto-rows: 1fr; gap: 16px; }
.oryx-primitive {
  padding: 20px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
  min-width: 0;
  overflow: hidden;
}
.oryx-primitive .oryx-code { flex: 1; }
.oryx-primitive:hover { border-color: var(--oryx-purple); box-shadow: 0 8px 24px rgba(109, 40, 217, 0.08); }
.oryx-primitive-header { display: flex; align-items: flex-start; gap: 12px; }
.oryx-primitive-icon { font-size: 28px; flex-shrink: 0; }
.oryx-primitive-title { font-size: 17px; font-weight: 700; color: #0f172a; margin: 0 0 2px; }
.oryx-primitive-subtitle { font-size: 12px; color: #94a3b8; margin: 0; }
.oryx-code {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  font-size: 12px;
  line-height: 1.6;
  color: #334155;
  overflow-x: auto;
  margin: 0;
  white-space: pre;
}
.oryx-code code { font-family: var(--vp-font-family-mono); background: none; color: inherit; }

/* ── Scenarios ── */
.oryx-scenarios { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.oryx-scenario {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
  transition: border-color 0.2s;
}
.oryx-scenario:hover { border-color: var(--oryx-purple-line); }
.oryx-scenario-num {
  font-size: 28px;
  font-weight: 900;
  color: var(--oryx-purple-line);
  line-height: 1;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}
.oryx-scenario-title { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0 0 6px; }
.oryx-scenario-desc { font-size: 13px; color: #64748b; line-height: 1.6; margin: 0; }

/* ── Tool tiers ── */
.oryx-sdk-section { background: #f8f7fc; }
.oryx-sdk-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.oryx-sdk-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.oryx-sdk-card-featured { border: 2px solid var(--oryx-purple); box-shadow: 0 10px 30px rgba(109, 40, 217, 0.1); }
.oryx-sdk-card-icon { font-size: 28px; }
.oryx-sdk-card-title { font-size: 17px; font-weight: 700; color: #0f172a; margin: 0; }
.oryx-sdk-card-desc { font-size: 14px; color: #64748b; line-height: 1.6; margin: 0; flex: 1; }
.oryx-langs { display: flex; flex-wrap: wrap; gap: 8px; }
.oryx-lang {
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}
.oryx-sdk-installs { display: flex; flex-direction: column; gap: 6px; }
.oryx-sdk-installs code {
  font-family: var(--vp-font-family-mono);
  font-size: 12px;
  background: var(--oryx-purple-soft);
  border: 1px solid var(--oryx-purple-line);
  border-radius: 6px;
  padding: 5px 10px;
  color: var(--oryx-purple-deep);
  display: block;
}
.oryx-sdk-badges { display: flex; flex-wrap: wrap; gap: 8px; }
.oryx-sdk-badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
}

/* ── API & CLI grid ── */
.oryx-proto-grid { display: flex; flex-direction: column; gap: 28px; }
.oryx-proto-group { display: flex; flex-direction: column; gap: 6px; }
.oryx-proto-group-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--oryx-purple-deep);
  margin-bottom: 4px;
}
.oryx-proto-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  padding: 8px 14px;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #e5e7eb;
  flex-wrap: wrap;
}
.oryx-proto-subject {
  font-family: var(--vp-font-family-mono);
  font-size: 12px;
  color: #0f172a;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
  white-space: nowrap;
}
.oryx-proto-desc { font-size: 13px; color: #64748b; flex: 1; }

/* ── CTA ── */
.oryx-cta-section { background: #f8f7fc; }
.oryx-cta { text-align: center; max-width: 680px; margin: 0 auto; }
.oryx-wip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px;
  border-radius: 20px;
  border: 1px solid #fcd34d;
  background: #fffbeb;
  color: #b45309;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 14px;
}
.oryx-wip-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse 2s infinite;
}
.oryx-cta-title { font-size: 28px; font-weight: 700; color: #0f172a; margin: 0 0 12px; }
.oryx-cta-desc { font-size: 15px; color: #64748b; margin: 0 0 24px; line-height: 1.6; }
.oryx-cta-code { text-align: left; margin-bottom: 28px; background: #ffffff; }
.oryx-cta-links { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }

/* ── Architecture diagram ── */
.oryx-flow-section { padding: 0 24px 72px; }
.oryx-flow-img {
  width: 100%;
  display: block;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

/* ── Footer ── */
.oryx-footer {
  padding: 28px 24px 36px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  border-top: 1px solid #e5e7eb;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .oryx-sdk-cards { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 768px) {
  .oryx-hero { padding: 64px 20px 56px; }
  .oryx-problem { grid-template-columns: minmax(0, 1fr); }
  .oryx-primitives { grid-template-columns: minmax(0, 1fr); grid-auto-rows: auto; }
  .oryx-scenarios { grid-template-columns: minmax(0, 1fr); }
  .oryx-proto-subject { white-space: normal; word-break: break-all; }
  .oryx-section { padding: 48px 20px; }
}
</style>
