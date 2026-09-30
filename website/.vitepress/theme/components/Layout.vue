<template>
  <DefaultTheme.Layout>
    <!-- 文档页底部挂评论；未配置 Giscus 时不渲染 -->
    <template #doc-after v-if="giscusEnabled && frontmatter.layout !== 'home'">
      <GiscusComment />
    </template>
  </DefaultTheme.Layout>
</template>

<script setup>
import DefaultTheme from 'vitepress/theme'
import { useData } from 'vitepress'
import GiscusComment from './GiscusComment.vue'
import { giscusEnabled } from '../../integrations'

const { frontmatter } = useData()
</script>
