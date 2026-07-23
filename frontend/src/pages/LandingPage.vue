<script setup>
import LandingHeader from "../components/landing/LandingHeader.vue";
import Hero from "../components/landing/Hero.vue";
import FeatureCard from "../components/landing/FeatureCard.vue";

const FEATURES = [
  {
    icon: "🚀",
    title: "원클릭 배포",
    description: "GitHub URL 또는 소스 파일 업로드만으로 배포 파이프라인이 자동으로 실행됩니다.",
  },
  {
    icon: "🛡️",
    title: "안전한 인프라 구성",
    description: "환경변수와 DB 비밀번호는 안전하게 암호화되어 전달되고 관리됩니다.",
  },
  {
    icon: "📊",
    title: "실시간 모니터링",
    description: "배포 상태와 서버 리소스 사용량을 대시보드에서 바로 확인할 수 있습니다.",
  },
];
</script>

<template>
  <div class="min-h-screen bg-background">
    <LandingHeader />
    <main>
      <Hero />
      <section class="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-6 pb-24 sm:grid-cols-3">
        <FeatureCard v-for="feature in FEATURES" :key="feature.title" v-bind="feature" />
      </section>
    </main>
  </div>
</template>
