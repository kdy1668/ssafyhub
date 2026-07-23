<script setup>
const { tone = "neutral" } = defineProps({
  tone: String,
});

const TONE_CLASSES = {
  neutral: "bg-black/5 text-ink-700",
  success: "bg-success-light text-success",
  warning: "bg-accent-light text-accent",
  danger: "bg-danger-light text-danger",
  info: "bg-primary-light text-primary",
};
</script>

<template>
  <span :class="['inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium', TONE_CLASSES[tone]]">
    <slot />
  </span>
</template>
