<script setup>
import { storeToRefs } from "pinia";
import { useToastStore } from "../../stores/useToastStore";

const toastStore = useToastStore();
const { toasts } = storeToRefs(toastStore);

const TONE_CLASSES = {
  error: "border-danger bg-danger-light text-danger",
  success: "border-success bg-success-light text-success",
  info: "border-primary bg-primary-light text-primary",
};
</script>

<template>
  <div v-if="toasts.length > 0" class="fixed right-4 top-4 z-[100] flex w-80 flex-col gap-2">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      role="alert"
      :class="[
        'flex items-start justify-between gap-3 rounded-md border px-4 py-3 text-sm shadow-popover',
        TONE_CLASSES[toast.variant] || TONE_CLASSES.error,
      ]"
    >
      <span>{{ toast.message }}</span>
      <button
        type="button"
        aria-label="닫기"
        class="text-current/70 hover:text-current"
        @click="toastStore.dismissToast(toast.id)"
      >
        ✕
      </button>
    </div>
  </div>
</template>
