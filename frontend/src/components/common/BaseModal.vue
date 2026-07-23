<script setup>
defineProps({
  open: Boolean,
  title: String,
  width: { type: String, default: "max-w-lg" },
});
defineEmits(["close"]);
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
    <div :class="['w-full rounded-lg border border-border bg-surface shadow-popover', width]">
      <div class="flex items-center justify-between border-b border-border px-5 py-3">
        <h2 class="text-base font-semibold text-ink-900">{{ title }}</h2>
        <button
          type="button"
          aria-label="닫기"
          class="rounded p-1 text-ink-500 hover:bg-black/5 hover:text-ink-900"
          @click="$emit('close')"
        >
          ✕
        </button>
      </div>
      <div class="max-h-[70vh] overflow-y-auto px-5 py-4">
        <slot />
      </div>
      <div v-if="$slots.footer" class="flex justify-end gap-2 border-t border-border px-5 py-3">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>
