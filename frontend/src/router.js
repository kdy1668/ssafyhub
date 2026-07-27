import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "./stores/useAuthStore";
import LoginPage from "./pages/LoginPage.vue";
import SignupPage from "./pages/SignupPage.vue";
import HomePage from "./pages/HomePage.vue";
import MyPage from "./pages/MyPage.vue";
import DeployRequestPage from "./pages/DeployRequestPage.vue";
import ManagePage from "./pages/ManagePage.vue";
import AdminPage from "./pages/AdminPage.vue";
import BoardListPage from "./pages/BoardListPage.vue";
import BoardWritePage from "./pages/BoardWritePage.vue";
import BoardDetailPage from "./pages/BoardDetailPage.vue";

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
    { path: "/admin", component: AdminPage, meta: { requiresAuth: true, requiresAdmin: true } },
    // 게시판 조회(목록/상세)는 비로그인 접근 허용, 작성/수정은 로그인 필요
    { path: "/board", component: BoardListPage },
    { path: "/board/new", component: BoardWritePage, meta: { requiresAuth: true } },
    { path: "/board/:id/edit", component: BoardWritePage, meta: { requiresAuth: true } },
    { path: "/board/:id", component: BoardDetailPage },
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
