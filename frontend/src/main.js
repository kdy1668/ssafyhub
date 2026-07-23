import { createApp } from "vue";
import { createPinia } from "pinia";
import { VueQueryPlugin, QueryCache, QueryClient } from "@tanstack/vue-query";
import App from "./App.vue";
import { router } from "./router";
import { useToastStore } from "./stores/useToastStore";
import "./index.css";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);

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
