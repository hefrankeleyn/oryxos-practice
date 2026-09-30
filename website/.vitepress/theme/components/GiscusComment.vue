<script setup>
import { onMounted, watch } from 'vue'
import { useData, useRoute } from 'vitepress'
import { giscus } from '../../integrations'

const route = useRoute()
const { lang } = useData()

/** 注入 giscus 客户端脚本；路由切换时重新加载，使每个页面对应各自的讨论。 */
function loadGiscus() {
  const container = document.getElementById('giscus-container')
  if (!container) return

  container.innerHTML = ''

  const script = document.createElement('script')
  script.src = 'https://giscus.app/client.js'
  script.setAttribute('data-repo', giscus.repo)
  script.setAttribute('data-repo-id', giscus.repoId)
  script.setAttribute('data-category', giscus.category)
  script.setAttribute('data-category-id', giscus.categoryId)
  script.setAttribute('data-mapping', 'pathname')
  script.setAttribute('data-strict', '0')
  script.setAttribute('data-reactions-enabled', '1')
  script.setAttribute('data-emit-metadata', '0')
  script.setAttribute('data-input-position', 'bottom')
  script.setAttribute('data-theme', 'light')
  script.setAttribute('data-lang', lang.value === 'zh-CN' ? 'zh-CN' : 'en')
  script.setAttribute('crossorigin', 'anonymous')
  script.async = true
  container.appendChild(script)
}

onMounted(() => loadGiscus())

watch(() => route.path, () => {
  setTimeout(loadGiscus, 300)
})
</script>

<template>
  <div style="margin-top: 48px; padding-top: 24px; border-top: 1px solid var(--vp-c-divider);">
    <div id="giscus-container" />
  </div>
</template>
