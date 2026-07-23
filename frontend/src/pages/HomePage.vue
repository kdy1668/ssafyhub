<script setup>
import HomeHeader from "../components/home/HomeHeader.vue";
import WelcomeBanner from "../components/home/WelcomeBanner.vue";
import ActionCard from "../components/home/ActionCard.vue";
</script>

<template>
  <div class="min-h-screen bg-background font-sans antialiased">
    <!-- 상단 헤더 -->
    <HomeHeader />

    <!-- 메인 컨텐츠 영역 -->
    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-10">
      <div class="space-y-8">
        
        <!-- 웰컴 배너 -->
        <WelcomeBanner />

        <!-- 주요 서비스 섹션 -->
        <section class="space-y-4">
          <!-- 섹션 타이틀 & 상태 인디케이터 -->
          <div class="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between border-b border-border/80 pb-3">
            <div>
              <h2 class="text-base sm:text-lg font-bold text-ink-900">주요 서비스 바로가기</h2>
              <p class="text-xs text-ink-500">프로젝트 배포 관리 및 커뮤니티 공간입니다.</p>
            </div>
            <span class="inline-flex w-fit items-center gap-1.5 rounded bg-primary-light px-2.5 py-1 text-xs font-semibold text-primary">
              <span class="h-2 w-2 rounded-full bg-primary animate-pulse"></span>
              DX Platform Normal
            </span>
          </div>

          <!-- 카드 그리드 (대형 화면에서 3열로 정돈) -->
          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <ActionCard
              icon="🚀"
              title="배포하기"
              description="새 프로젝트를 등록하고 AWS에 배포를 요청합니다."
              to="/deploy/new"
            />
            <ActionCard
              icon="📦"
              title="내 배포 서비스 보기"
              description="현재 배포된 서비스 목록과 상태를 확인합니다."
              to="/deployments"
            />
            <ActionCard
              icon="💬"
              title="자유게시판"
              description="자유롭게 글을 쓰고 다른 사용자와 소통합니다."
              to="/board"
            />
          </div>
        </section>

      </div>
    </main>
  </div>
</template>