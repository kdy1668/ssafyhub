<script setup>
// AWS 콘솔 마법사(예: Fargate 태스크 생성) 스타일의 단계 번호 헤더.
defineProps({
  step: { type: [Number, String], required: true },
  title: { type: String, required: true },
});
</script>

<template>
  <div class="flex items-center gap-2">
    <span
      class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-bold text-white"
    >
      {{ step }}
    </span>
    <h2 class="text-sm font-bold text-ink-900">{{ title }}</h2>
  </div>
</template>
