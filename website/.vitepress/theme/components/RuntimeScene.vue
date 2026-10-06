<script setup>
defineProps({ isZh: { type: Boolean, default: true } })
</script>

<template>
  <svg class="runtime-scene" viewBox="0 0 620 520" fill="none" aria-hidden="true">
    <!-- 文件定义与运行底座的分层示意，不是实时执行界面。 -->
    <ellipse cx="318" cy="460" rx="235" ry="24" fill="#292536" opacity=".055" />
    <path d="M36 370V432L111 469M515 461L590 424V363" stroke="#CEC9D6" />
    <path d="m43 385 7-3m-7 16 7-3m-7 16 7-3m531-16 7 3m-7 10 7 3m-7 10 7 3" stroke="#CEC9D6" />
    <path d="m65 318 283-93 217 104-283 96Z" fill="#DCD5EC" stroke="#9183AE" stroke-width="1.2" stroke-linejoin="round" />
    <path d="m65 318 217 107v29L65 347Z" fill="#C8BFDE" stroke="#9183AE" stroke-width="1.2" stroke-linejoin="round" />
    <path d="m282 425 283-96v29l-283 96Z" fill="#6650B8" stroke="#5B479F" stroke-width="1.2" stroke-linejoin="round" />
    <text x="117" y="361" transform="rotate(26 117 361)" fill="#403455" font-size="30" font-weight="600" letter-spacing="-1">OryxOS</text>
    <text x="414" y="400" transform="rotate(-19 414 400)" fill="#FFFFFF" font-size="14" font-weight="500">Java 21</text>
    <path d="m322 423 19-6m-19 13 19-6m8-10 19-6m-19 13 19-6" stroke="#C4B8E8" stroke-width="2" />
    <path d="m91 262 257-85 191 92-257 88Z" fill="#FFF" stroke="#A79CB9" stroke-width="1.2" stroke-linejoin="round" />
    <path d="m91 262 191 95v16L91 278Z" fill="#EFF2EC" stroke="#A79CB9" stroke-width="1.2" stroke-linejoin="round" />
    <path d="m282 357 257-88v16l-257 88Z" fill="#D2E5DC" stroke="#9BAD9F" stroke-width="1.2" stroke-linejoin="round" />
    <path d="m126 267 38 19m8 4 38 19m8 4 38 19" stroke="#E0DAEA" stroke-width="5" />
    <path d="m292 334 177-60" stroke="#B1CBBE" stroke-width="3" />
    <path d="m301 343 177-60" stroke="#C4D8CE" stroke-width="2" />
    <path d="M154 229v28l75 37M321 201v70M482 246v19l-88 31" stroke="#9C8EB0" stroke-width="1.3" stroke-dasharray="4 5" />
    <circle cx="229" cy="294" r="4" fill="#6650B8" stroke="#FFF" stroke-width="2" />
    <circle cx="321" cy="271" r="4" fill="#6650B8" stroke="#FFF" stroke-width="2" />
    <circle cx="394" cy="296" r="4" fill="#6650B8" stroke="#FFF" stroke-width="2" />
    <g>
      <path d="M86 99h31l10 10h91v117H86Z" fill="#B9D5C6" stroke="#789E8C" stroke-width="1.2" stroke-linejoin="round" />
      <path d="M80 126h146v101a8 8 0 0 1-8 8H88a8 8 0 0 1-8-8Z" fill="#D2E5DC" stroke="#789E8C" stroke-width="1.2" />
      <path d="M101 134V85h79l20 20v67h-99Z" fill="#F9FCFA" stroke="#8BAB9B" stroke-width="1.1" stroke-linejoin="round" />
      <path d="M180 85v20h20" fill="#DFEDE4" stroke="#8BAB9B" stroke-width="1.1" />
      <path d="M116 111h40m-40 10h56m-56 10h47" stroke="#B6CCC1" stroke-width="2" />
      <rect x="80" y="146" width="146" height="89" rx="8" fill="#D2E5DC" />
      <path d="M80 154v73a8 8 0 0 0 8 8h130a8 8 0 0 0 8-8v-73" stroke="#789E8C" stroke-width="1.2" />
      <text x="99" y="174" fill="#466957" font-size="12">{{ isZh ? '每日天气' : 'Weather' }}</text>
      <text x="99" y="204" fill="#2C4638" font-size="18" font-weight="600">AGENT.md</text>
      <circle cx="209" cy="131" r="3" fill="#87B39B" />
    </g>
    <g>
      <path d="M251 62h31l10 10h103v117H251Z" fill="#CABFE0" stroke="#9683B5" stroke-width="1.2" stroke-linejoin="round" />
      <path d="M245 89h158v108a8 8 0 0 1-8 8H253a8 8 0 0 1-8-8Z" fill="#E4DFF1" stroke="#9683B5" stroke-width="1.2" />
      <path d="M269 95V42h84l21 21v73H269Z" fill="#FCFBFE" stroke="#A496BA" stroke-width="1.1" stroke-linejoin="round" />
      <path d="M353 42v21h21" fill="#EAE5F3" stroke="#A496BA" stroke-width="1.1" />
      <path d="M285 70h39m-39 10h58m-58 10h47" stroke="#CBC1DC" stroke-width="2" />
      <rect x="245" y="110" width="158" height="95" rx="8" fill="#E4DFF1" />
      <path d="M245 118v79a8 8 0 0 0 8 8h142a8 8 0 0 0 8-8v-79" stroke="#9683B5" stroke-width="1.2" />
      <text x="265" y="141" fill="#756090" font-size="12">{{ isZh ? '科技日报' : 'Tech digest' }}</text>
      <text x="265" y="172" fill="#493759" font-size="18" font-weight="600">AGENT.md</text>
      <path d="M361 91h24" stroke="#9E87BC" stroke-width="2" />
    </g>
    <g>
      <path d="M424 123h28l10 10h86v111H424Z" fill="#DDD9E5" stroke="#A79CB9" stroke-width="1.2" stroke-linejoin="round" />
      <path d="M418 148h138v99a8 8 0 0 1-8 8H426a8 8 0 0 1-8-8Z" fill="#F5F2F8" stroke="#A79CB9" stroke-width="1.2" />
      <path d="M439 156v-52h75l19 19v71h-94Z" fill="#FFF" stroke="#AEA4BA" stroke-width="1.1" stroke-linejoin="round" />
      <path d="M514 104v19h19" fill="#EAE5F0" stroke="#AEA4BA" stroke-width="1.1" />
      <path d="M453 133h36m-36 10h50m-50 10h43" stroke="#D3CBDD" stroke-width="2" />
      <rect x="418" y="170" width="138" height="85" rx="8" fill="#F5F2F8" />
      <path d="M418 178v69a8 8 0 0 0 8 8h122a8 8 0 0 0 8-8v-69" stroke="#A79CB9" stroke-width="1.2" />
      <text x="436" y="196" fill="#81748E" font-size="12">{{ isZh ? 'GitHub 日报' : 'GitHub digest' }}</text>
      <text x="436" y="226" fill="#4E405E" font-size="18" font-weight="600">AGENT.md</text>
    </g>
    <path d="M27 288h48l22-12M578 305h-30l-12-13" stroke="#ACA0BD" stroke-width="1.1" />
    <text x="27" y="310" fill="#81748E" font-size="11">{{ isZh ? '共享执行层' : 'Shared execution' }}</text>
    <text x="427" y="479" fill="#81748E" font-size="11">{{ isZh ? '定义在上 · 运行在下' : 'Definitions above. Runtime below.' }}</text>
  </svg>
</template>

<style scoped>
.runtime-scene { display: block; width: 100%; height: auto; overflow: visible; font-family: 'Avenir Next', 'Trebuchet MS', 'PingFang SC', 'Microsoft YaHei', sans-serif; }
</style>
