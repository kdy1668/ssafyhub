<script setup>
import BaseSkeleton from "../common/BaseSkeleton.vue";
import { useCurrentUser } from "../../composables/useCurrentUser";

const { data: user, isLoading, isError } = useCurrentUser();
</script>

<template>
  <div v-if="isLoading" class="mb-8 flex flex-col gap-2">
    <BaseSkeleton class="h-7 w-64" />
    <BaseSkeleton class="h-4 w-40" />
  </div>
  <div v-else-if="isError" class="mb-8">
    <h1 class="text-2xl font-semibold text-ink-900">안녕하세요</h1>
  </div>
  <div v-else class="mb-8">
    <h1 class="text-2xl font-semibold text-ink-900">{{ user.name }}님, 안녕하세요</h1>
    <p class="text-sm text-ink-500">{{ user.team }}</p>
  </div>
</template>
