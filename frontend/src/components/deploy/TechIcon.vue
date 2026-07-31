<script setup>
// 배포 신청 화면의 DB/백킹서비스 선택 카드용 아이콘.
// 실제 제품 로고 대신 색상 + 추상 아이콘으로 기술을 구분한다(상표 문제 회피 + 유지보수 단순화).
const props = defineProps({
  kind: { type: String, required: true }, // none | mysql | redis | mongodb | java | python | node | react | vue | angular | svelte | plain
});

const PALETTE = {
  none: { bg: "#F1F5F9", fg: "#94A3B8" },
  mysql: { bg: "#E7F3FE", fg: "#00618A" },
  redis: { bg: "#FCE8E6", fg: "#C5221F" },
  mongodb: { bg: "#E6F4EA", fg: "#12924F" },
  java: { bg: "#FBF1E1", fg: "#C77700" },
  python: { bg: "#E7F3FE", fg: "#1F8CE6" },
  node: { bg: "#E6F4EA", fg: "#137333" },
  react: { bg: "#E7F3FE", fg: "#0B72D9" },
  vue: { bg: "#E6F4EA", fg: "#12924F" },
  angular: { bg: "#FCE8E6", fg: "#C5221F" },
  svelte: { bg: "#FBF1E1", fg: "#C77700" },
  plain: { bg: "#F1F5F9", fg: "#64748B" },
};

const colors = PALETTE[props.kind] || PALETTE.none;
</script>

<template>
  <span
    class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
    :style="{ backgroundColor: colors.bg }"
  >
    <!-- 사용 안 함 -->
    <svg v-if="kind === 'none'" viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2">
      <circle cx="12" cy="12" r="9" />
      <line x1="7" y1="7" x2="17" y2="17" />
    </svg>

    <!-- MySQL: 원통형 DB 아이콘 -->
    <svg
      v-else-if="kind === 'mysql'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <ellipse cx="12" cy="6" rx="7" ry="3" />
      <path d="M5 6v6c0 1.66 3.13 3 7 3s7-1.34 7-3V6" />
      <path d="M5 12v6c0 1.66 3.13 3 7 3s7-1.34 7-3v-6" />
    </svg>

    <!-- Redis: 번개(캐시/속도) -->
    <svg v-else-if="kind === 'redis'" viewBox="0 0 24 24" class="h-5 w-5" :fill="colors.fg">
      <path d="M13 2 3 14h7l-1 8 11-14h-8z" />
    </svg>

    <!-- MongoDB: 잎사귀(문서형 DB) -->
    <svg
      v-else-if="kind === 'mongodb'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M12 3c4 3 6 7 6 11a6 6 0 0 1-12 0c0-4 2-8 6-11z" />
      <line x1="12" y1="14" x2="12" y2="21" />
    </svg>

    <!-- Java: 컵(커피) -->
    <svg
      v-else-if="kind === 'java'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M5 9h11v6a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4V9z" />
      <path d="M16 10h1.5a2.5 2.5 0 0 1 0 5H16" />
      <line x1="8" y1="3" x2="8" y2="5" />
      <line x1="12" y1="3" x2="12" y2="5" />
    </svg>

    <!-- Python: 맞물린 두 방울 -->
    <svg
      v-else-if="kind === 'python'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M12 3c-3 0-4 1-4 3v2h5" />
      <rect x="4" y="6" width="8" height="6" rx="2" />
      <path d="M12 21c3 0 4-1 4-3v-2H11" />
      <rect x="12" y="12" width="8" height="6" rx="2" />
    </svg>

    <!-- Node.js: 육각형 -->
    <svg
      v-else-if="kind === 'node'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M12 2 21 7v10l-9 5-9-5V7z" />
      <path d="M12 8v8" />
    </svg>

    <!-- React: 교차하는 두 궤도 -->
    <svg
      v-else-if="kind === 'react'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="1.6"
    >
      <circle cx="12" cy="12" r="2.2" :fill="colors.fg" stroke="none" />
      <ellipse cx="12" cy="12" rx="9" ry="3.6" />
      <ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(60 12 12)" />
      <ellipse cx="12" cy="12" rx="9" ry="3.6" transform="rotate(120 12 12)" />
    </svg>

    <!-- Vue: 쐐기(V) -->
    <svg v-else-if="kind === 'vue'" viewBox="0 0 24 24" class="h-5 w-5" :fill="colors.fg">
      <path d="M2 4h4.2L12 14l5.8-10H22L12 21z" />
    </svg>

    <!-- Angular: 방패형 배지 -->
    <svg
      v-else-if="kind === 'angular'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M12 2 21 6l-1.5 12L12 22l-7.5-4L3 6z" />
      <path d="M8 15h8M12 6l4 10M12 6 8 16" />
    </svg>

    <!-- Svelte: 겹쳐진 두 곡선(민첩함) -->
    <svg
      v-else-if="kind === 'svelte'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round"
    >
      <path d="M7 15c-2 2-2 5 0 6.5 1.7 1.3 3.9 1 5.3-.6l4-4.6" />
      <path d="M17 9c2-2 2-5 0-6.5-1.7-1.3-3.9-1-5.3.6l-4 4.6" />
    </svg>

    <!-- PLAIN(이미 빌드됨): 문서/파일 -->
    <svg
      v-else-if="kind === 'plain'"
      viewBox="0 0 24 24" class="h-5 w-5" fill="none" :stroke="colors.fg" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
    >
      <path d="M7 3h7l4 4v14H7z" />
      <path d="M14 3v4h4" />
      <line x1="9.5" y1="13" x2="14.5" y2="13" />
      <line x1="9.5" y1="17" x2="14.5" y2="17" />
    </svg>
  </span>
</template>
