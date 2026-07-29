<script setup>
import { computed } from "vue";
import BaseSkeleton from "../common/BaseSkeleton.vue";

const props = defineProps({
  data: { type: Object, default: null }, // { cpuPercent, memoryUsedMb, memoryTotalMb, sampledAt }
  loading: { type: Boolean, default: false },
});

function clamp(p) {
  return Math.max(0, Math.min(100, p));
}

const cpu = computed(() => (props.data ? clamp(props.data.cpuPercent) : 0));
const memPercent = computed(() => {
  if (!props.data?.memoryTotalMb) return 0;
  return clamp(Math.round((props.data.memoryUsedMb / props.data.memoryTotalMb) * 100));
});

// 사용률에 따른 막대 색상 (60/85 임계)
function barColor(p) {
  if (p >= 85) return "bg-danger";
  if (p >= 60) return "bg-accent";
  return "bg-primary";
}

const sampledLabel = computed(() =>
  props.data?.sampledAt ? new Date(props.data.sampledAt).toLocaleTimeString("ko-KR") : ""
);
</script>

<template>
  <div class="rounded-lg border border-border bg-surface p-5 shadow-card">
    <div class="mb-4 flex items-center justify-between">
      <h3 class="flex items-center gap-2 text-sm font-bold text-ink-900">
        리소스 메트릭
        <span class="flex items-center gap-1 text-xs font-normal text-ink-500">
          <span class="h-1.5 w-1.5 rounded-full bg-success" />
          live
        </span>
      </h3>
      <span v-if="sampledLabel" class="text-xs text-ink-500">{{ sampledLabel }} 기준</span>
    </div>

    <div v-if="loading && !data" class="flex flex-col gap-4">
      <BaseSkeleton class="h-12 w-full rounded-md" />
      <BaseSkeleton class="h-12 w-full rounded-md" />
    </div>

    <div v-else class="grid grid-cols-1 gap-5 sm:grid-cols-2">
      <!-- CPU -->
      <div>
        <div class="mb-1.5 flex items-baseline justify-between">
          <span class="text-xs font-medium text-ink-500">CPU 사용률</span>
          <span class="text-lg font-bold tabular-nums text-ink-900">{{ cpu }}%</span>
        </div>
        <div class="h-2 w-full overflow-hidden rounded-full bg-black/10">
          <div class="h-2 rounded-full transition-all duration-500" :class="barColor(cpu)" :style="{ width: `${cpu}%` }" />
        </div>
      </div>

      <!-- Memory -->
      <div>
        <div class="mb-1.5 flex items-baseline justify-between">
          <span class="text-xs font-medium text-ink-500">메모리</span>
          <span class="text-lg font-bold tabular-nums text-ink-900">
            {{ data?.memoryUsedMb ?? 0 }}<span class="text-xs font-normal text-ink-500"> / {{ data?.memoryTotalMb ?? 0 }}MB</span>
          </span>
        </div>
        <div class="h-2 w-full overflow-hidden rounded-full bg-black/10">
          <div class="h-2 rounded-full transition-all duration-500" :class="barColor(memPercent)" :style="{ width: `${memPercent}%` }" />
        </div>
      </div>
    </div>
  </div>
</template>
