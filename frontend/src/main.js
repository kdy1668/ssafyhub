import { createApp } from "vue";
import { createPinia } from "pinia";
import { VueQueryPlugin, QueryCache, QueryClient } from "@tanstack/vue-query";
import App from "./App.vue";
import { router } from "./router";
import { useToastStore } from "./stores/useToastStore";
import { useAuthStore } from "./stores/useAuthStore";
import { ensureCsrfToken, setOnSessionExpired } from "./api/client";
import "./index.css";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);

// 재발급 실패(세션 만료) 시: 인증 상태 초기화 → 안내 → 로그인 페이지 이동
setOnSessionExpired(() => {
  useAuthStore().logout();
  useToastStore().showToast("로그인이 만료되었습니다. 다시 로그인해 주세요.", "error");
  router.push("/login");
});

// 앱 시작 시 CSRF 토큰 발급 (XSRF-TOKEN 쿠키가 없을 때만 요청)
ensureCsrfToken();

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30 * 1000,
    },
    mutations: {
      onError: (error) => {
        useToastStore().showToast(error.message, "error");
      },
    },
  },
  queryCache: new QueryCache({
    onError: (error) => {
      useToastStore().showToast(error.message, "error");
    },
  }),
});

app.use(VueQueryPlugin, { queryClient });

app.mount("#app");
