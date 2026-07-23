<script setup>
import BaseCard from "../common/BaseCard.vue";

defineProps({
  icon: String,
  title: String,
  description: String,
});
</script>

<template>
  <BaseCard class="flex flex-col gap-3 p-6">
    <span class="text-2xl">{{ icon }}</span>
    <h3 class="text-base font-semibold text-ink-900">{{ title }}</h3>
    <p class="text-sm text-ink-500">{{ description }}</p>
  </BaseCard>
</template>
