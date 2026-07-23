<script setup>
import { useRouter } from "vue-router";
import BaseCard from "../common/BaseCard.vue";

const props = defineProps({
  icon: String,
  title: String,
  description: String,
  to: String,
});

const router = useRouter();

function navigate() {
  router.push(props.to);
}
</script>

<template>
  <BaseCard
    role="button"
    tabindex="0"
    class="flex cursor-pointer flex-col gap-3 p-8 transition-shadow hover:shadow-popover"
    @click="navigate"
    @keydown.enter="navigate"
  >
    <span class="text-3xl">{{ icon }}</span>
    <h2 class="text-lg font-semibold text-ink-900">{{ title }}</h2>
    <p class="text-sm text-ink-500">{{ description }}</p>
  </BaseCard>
</template>
