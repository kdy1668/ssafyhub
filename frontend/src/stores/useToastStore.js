import { defineStore } from "pinia";

let nextId = 1;

export const useToastStore = defineStore("toast", {
  state: () => ({
    toasts: [],
  }),
  actions: {
    showToast(message, variant = "error") {
      const id = nextId++;
      this.toasts.push({ id, message, variant });
      setTimeout(() => {
        this.dismissToast(id);
      }, 4000);
    },
    dismissToast(id) {
      this.toasts = this.toasts.filter((t) => t.id !== id);
    },
  },
});
