import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "./stores/useAuthStore";
import LoginPage from "./pages/LoginPage.vue";
import SignupPage from "./pages/SignupPage.vue";
import HomePage from "./pages/HomePage.vue";
import MyPage from "./pages/MyPage.vue";
import DeployRequestPage from "./pages/DeployRequestPage.vue";
import ManagePage from "./pages/ManagePage.vue";
import ResourcesPage from "./pages/ResourcesPage.vue";
import EnvUpdatePage from "./pages/EnvUpdatePage.vue";
import RuntimeLogsPage from "./pages/RuntimeLogsPage.vue";
import AdminPage from "./pages/AdminPage.vue";
import ShowcaseListPage from "./pages/ShowcaseListPage.vue";
import ShowcaseDetailPage from "./pages/ShowcaseDetailPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 공통 메인: 로그인/비로그인 모두 동일한 피드 화면
    { path: "/", component: HomePage },
    { path: "/home", redirect: "/" }, // 이전 경로 호환
    { path: "/signup", component: SignupPage },
    { path: "/login", component: LoginPage },
    { path: "/mypage", component: MyPage, meta: { requiresAuth: true } },
    { path: "/deploy/new", component: DeployRequestPage, meta: { requiresAuth: true } },
    { path: "/deployments", component: ManagePage, meta: { requiresAuth: true } },
    { path: "/deployments/resources", component: ResourcesPage, meta: { requiresAuth: true } },
    { path: "/deployments/env", component: EnvUpdatePage, meta: { requiresAuth: true } },
    { path: "/deployments/logs", component: RuntimeLogsPage, meta: { requiresAuth: true } },
    { path: "/admin", component: AdminPage, meta: { requiresAuth: true, requiresAdmin: true } },
    // 쇼케이스(공개 배포 목록/상세)는 백엔드 API 자체가 인증 불필요이므로 비로그인 접근 허용
    { path: "/showcase", component: ShowcaseListPage },
    { path: "/showcase/:id", component: ShowcaseDetailPage },
  ],
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const authStore = useAuthStore();
    if (!authStore.isAuthenticated) {
      return { path: "/login", query: { from: to.fullPath } };
    }
    // 관리자 전용 페이지 접근 차단
    if (to.meta.requiresAdmin && !authStore.isAdmin) {
      return { path: "/home" };
    }
  }
});
