import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "./stores/useAuthStore";
import LandingPage from "./pages/LandingPage.vue";
import LoginPage from "./pages/LoginPage.vue";
import SignupPage from "./pages/SignupPage.vue";
import HomePage from "./pages/HomePage.vue";
import DeployRequestPage from "./pages/DeployRequestPage.vue";
import ManagePage from "./pages/ManagePage.vue";
import BoardListPage from "./pages/BoardListPage.vue";
import BoardWritePage from "./pages/BoardWritePage.vue";
import BoardDetailPage from "./pages/BoardDetailPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: LandingPage },
    { path: "/signup", component: SignupPage },
    { path: "/login", component: LoginPage },
    { path: "/home", component: HomePage, meta: { requiresAuth: true } },
    { path: "/deploy/new", component: DeployRequestPage, meta: { requiresAuth: true } },
    { path: "/deployments", component: ManagePage, meta: { requiresAuth: true } },
    { path: "/board", component: BoardListPage, meta: { requiresAuth: true } },
    { path: "/board/new", component: BoardWritePage, meta: { requiresAuth: true } },
    { path: "/board/:id/edit", component: BoardWritePage, meta: { requiresAuth: true } },
    { path: "/board/:id", component: BoardDetailPage, meta: { requiresAuth: true } },
  ],
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const authStore = useAuthStore();
    if (!authStore.isAuthenticated) {
      return { path: "/login", query: { from: to.fullPath } };
    }
  }
});
