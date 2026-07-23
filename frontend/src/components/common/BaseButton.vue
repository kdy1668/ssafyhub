<script setup>
const {
  variant = "primary",
  loading = false,
  disabled = false,
  type = "button",
} = defineProps({
  variant: String,
  loading: Boolean,
  disabled: Boolean,
  type: String,
});

defineEmits(["click"]);

const VARIANT_CLASSES = {
  primary: "bg-primary text-white hover:bg-primary-hover disabled:bg-primary/50",
  secondary:
    "bg-surface text-primary border border-primary/40 hover:bg-primary-light disabled:opacity-50",
  ghost: "bg-transparent text-ink-700 hover:bg-black/5 disabled:opacity-50",
  danger: "bg-danger text-white hover:bg-danger/90 disabled:bg-danger/50",
};
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed',
      VARIANT_CLASSES[variant],
    ]"
    @click="$emit('click', $event)"
  >
    <span
      v-if="loading"
      class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
    />
    <slot />
  </button>
</template>
