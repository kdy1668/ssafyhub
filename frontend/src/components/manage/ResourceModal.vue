<script setup>
import { toRef } from "vue";
import BaseModal from "../common/BaseModal.vue";
import BaseSkeleton from "../common/BaseSkeleton.vue";
import { useDeploymentResources } from "../../composables/useDeploymentResources";

const props = defineProps({
  open: Boolean,
  deploymentId: String,
  projectName: String,
});
defineEmits(["close"]);

const { data, isLoading } = useDeploymentResources(
  toRef(props, "deploymentId"),
  toRef(props, "open")
);

function clampPercent(percent) {
  return Math.min(percent, 100);
}
</script>

<template>
  <BaseModal :open="open" :title="`${projectName} 리소스 사용량`" @close="$emit('close')">
    <div v-if="isLoading || !data" class="flex flex-col gap-3">
      <BaseSkeleton class="h-5 w-full" />
      <BaseSkeleton class="h-5 w-full" />
    </div>
    <div v-else class="flex flex-col gap-4">
      <div>
        <div class="mb-1 flex justify-between text-sm">
          <span class="font-medium text-ink-700">CPU 사용률</span>
          <span class="text-ink-900">{{ data.cpuPercent }}%</span>
        </div>
        <div class="h-2 w-full rounded-full bg-black/10">
          <div
            class="h-2 rounded-full bg-primary"
            :style="{ width: `${clampPercent(data.cpuPercent)}%` }"
          />
        </div>
      </div>
      <div>
        <div class="mb-1 flex justify-between text-sm">
          <span class="font-medium text-ink-700">메모리 사용량</span>
          <span class="text-ink-900">{{ data.memoryUsedMb }}MB / {{ data.memoryTotalMb }}MB</span>
        </div>
        <div class="h-2 w-full rounded-full bg-black/10">
          <div
            class="h-2 rounded-full bg-primary"
            :style="{ width: `${clampPercent((data.memoryUsedMb / data.memoryTotalMb) * 100)}%` }"
          />
        </div>
      </div>
      <p class="text-xs text-ink-500">측정 시각: {{ new Date(data.sampledAt).toLocaleString() }}</p>
    </div>
  </BaseModal>
</template>
