<script setup>
import { RouterLink } from "vue-router";
import BaseButton from "../common/BaseButton.vue";
</script>

<template>
  <section class="mx-auto flex max-w-6xl flex-col items-center gap-6 px-6 py-24 text-center">
    <h1 class="text-4xl font-bold text-ink-900">버튼 하나로, 내 프로젝트를 AWS에 배포하세요</h1>
    <p class="max-w-2xl text-base text-ink-500">
      GitHub 저장소나 소스 파일만 준비하면 인프라 설정 없이 몇 분 안에 배포가 완료됩니다.
    </p>
    <RouterLink to="/signup">
      <BaseButton class="px-6 py-3 text-base">시작하기</BaseButton>
    </RouterLink>
  </section>
</template>
