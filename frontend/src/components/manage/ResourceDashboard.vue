<script setup>
import { computed, toRef } from "vue";
import BaseSkeleton from "../common/BaseSkeleton.vue";
import MetricChart from "./MetricChart.vue";
import { useDeploymentResources } from "../../composables/useDeploymentResources";
import { useDeploymentResourceHistory } from "../../composables/useDeploymentResourceHistory";
import { formatBytes, formatCount } from "../../utils/format";

const props = defineProps({
  deploymentId: String,
  enabled: { type: Boolean, default: true },
});

const { data, isLoading } = useDeploymentResources(
  toRef(props, "deploymentId"),
  toRef(props, "enabled")
);
const { data: history } = useDeploymentResourceHistory(
  toRef(props, "deploymentId"),
  toRef(props, "enabled")
);

function clamp(p) {
  return Math.max(0, Math.min(100, p));
}

function barColor(p) {
  if (p >= 85) return "bg-danger";
  if (p >= 60) return "bg-accent";
  return "bg-primary";
}

const cpuPercent = computed(() => (data.value ? clamp(data.value.cpuPercent) : 0));
const memPercent = computed(() => {
  if (!data.value?.memoryTotalMb) return 0;
  return clamp(Math.round((data.value.memoryUsedMb / data.value.memoryTotalMb) * 100));
});

// 상단 스탯 카드 정의. CPU/메모리는 상한이 있어 진행률 바를 곁들이고,
// 네트워크/요청수는 고정 상한이 없는 누적값이라 큰 숫자 위주(Prometheus stat 패널 스타일).
const stats = computed(() => {
  const d = data.value;
  return [
    {
      key: "cpu",
      label: "CPU 사용률",
      value: `${cpuPercent.value}%`,
      percent: cpuPercent.value,
    },
    {
      key: "memory",
      label: "메모리",
      value: `${d?.memoryUsedMb ?? 0} / ${d?.memoryTotalMb ?? 0}MB`,
      percent: memPercent.value,
    },
    {
      key: "netRx",
      label: "네트워크 수신 (누적)",
      value: formatBytes(d?.networkReceiveBytes),
    },
    {
      key: "netTx",
      label: "네트워크 송신 (누적)",
      value: formatBytes(d?.networkTransmitBytes),
    },
    {
      key: "requests",
      label: "누적 요청 수",
      value: formatCount(d?.requestsTotal),
    },
  ];
});

const charts = computed(() => [
  { key: "cpu", label: "CPU 사용률", unit: "%", color: "#3b82f6", max: 100, points: history.value?.cpu || [] },
  {
    key: "memory",
    label: "메모리 사용량",
    unit: "MB",
    color: "#10b981",
    max: data.value?.memoryTotalMb,
    points: history.value?.memory || [],
  },
  {
    key: "netRx",
    label: "네트워크 수신",
    unit: "MB",
    color: "#6366f1",
    max: 0,
    points: history.value?.networkReceive || [],
  },
  {
    key: "netTx",
    label: "네트워크 송신",
    unit: "MB",
    color: "#8b5cf6",
    max: 0,
    points: history.value?.networkTransmit || [],
  },
  { key: "requests", label: "누적 요청 수", unit: "회", color: "#ec4899", max: 0, points: history.value?.requests || [] },
]);

const sampledLabel = computed(() =>
  data.value?.sampledAt ? new Date(data.value.sampledAt).toLocaleTimeString("ko-KR") : ""
);
</script>

<template>
  <div class="flex flex-col gap-6">
    <div v-if="isLoading && !data" class="flex flex-col gap-4">
      <BaseSkeleton class="h-20 w-full rounded-lg" />
      <BaseSkeleton class="h-20 w-full rounded-lg" />
    </div>

    <template v-else>
      <div class="flex items-center justify-between">
        <span class="flex items-center gap-1.5 text-xs font-medium text-ink-500">
          <span class="h-1.5 w-1.5 rounded-full bg-success" />
          live
        </span>
        <span v-if="sampledLabel" class="text-xs text-ink-500">{{ sampledLabel }} 기준</span>
      </div>

      <!-- 스탯 카드 그리드 -->
      <div class="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <div
          v-for="stat in stats"
          :key="stat.key"
          class="rounded-lg border border-border bg-surface p-4 shadow-card"
        >
          <span class="block text-xs font-medium text-ink-500">{{ stat.label }}</span>
          <span class="mt-1 block text-lg font-bold tabular-nums text-ink-900">{{ stat.value }}</span>
          <div v-if="stat.percent !== undefined" class="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-black/10">
            <div
              class="h-1.5 rounded-full transition-all duration-500"
              :class="barColor(stat.percent)"
              :style="{ width: `${stat.percent}%` }"
            />
          </div>
        </div>
      </div>

      <!-- 시계열 그래프 -->
      <div class="flex flex-col gap-3">
        <span class="text-xs font-semibold text-ink-500">최근 사용량 추이</span>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <MetricChart
            v-for="chart in charts"
            :key="chart.key"
            :points="chart.points"
            :label="chart.label"
            :unit="chart.unit"
            :color="chart.color"
            :max="chart.max"
          />
        </div>
      </div>
    </template>
  </div>
</template>
