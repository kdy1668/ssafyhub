<script setup>
import { computed } from "vue";

// 의존성 없는 SVG 라인차트. points: [{ time(초), value }].
const props = defineProps({
  points: { type: Array, default: () => [] },
  label: { type: String, default: "" },
  unit: { type: String, default: "" }, // "%", "MB"
  color: { type: String, default: "#3b82f6" },
  max: { type: Number, default: 0 }, // 0이면 데이터 최대값 기준 자동 스케일
});

const W = 300;
const H = 100;
const PAD = 4;

function fmtTime(t) {
  return new Date(t * 1000).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

const view = computed(() => {
  const pts = props.points || [];
  if (pts.length === 0) return { empty: true };

  const values = pts.map((p) => p.value ?? 0);
  const dataMax = Math.max(...values, 0);
  const scaleMax = props.max > 0 ? props.max : dataMax > 0 ? dataMax * 1.2 : 1;
  const n = pts.length;

  const coords = pts.map((p, i) => {
    const x = PAD + (n === 1 ? (W - 2 * PAD) / 2 : (i / (n - 1)) * (W - 2 * PAD));
    const y = H - PAD - ((p.value ?? 0) / scaleMax) * (H - 2 * PAD);
    return [x, y];
  });

  const line = coords.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(" ");
  const area = `${PAD.toFixed(1)},${(H - PAD).toFixed(1)} ${line} ${coords[n - 1][0].toFixed(1)},${(H - PAD).toFixed(1)}`;

  return {
    empty: false,
    line,
    area,
    last: values[n - 1],
    scaleMax,
    first: fmtTime(pts[0].time),
    lastTime: fmtTime(pts[n - 1].time),
  };
});
</script>

<template>
  <div class="rounded-lg border border-border bg-surface p-3">
    <div class="mb-1 flex items-baseline justify-between">
      <span class="text-xs font-medium text-ink-600">{{ label }}</span>
      <span v-if="!view.empty" class="text-sm font-bold" :style="{ color }">
        {{ Math.round(view.last * 10) / 10 }}{{ unit }}
      </span>
    </div>

    <div v-if="view.empty" class="flex h-24 items-center justify-center text-xs text-ink-400">
      아직 데이터가 없습니다
    </div>
    <template v-else>
      <svg viewBox="0 0 300 100" preserveAspectRatio="none" class="h-24 w-full">
        <polygon :points="view.area" :fill="color" fill-opacity="0.12" />
        <polyline
          :points="view.line"
          fill="none"
          :stroke="color"
          stroke-width="1.5"
          vector-effect="non-scaling-stroke"
          stroke-linejoin="round"
          stroke-linecap="round"
        />
      </svg>
      <div class="mt-1 flex justify-between text-[10px] text-ink-400">
        <span>{{ view.first }}</span>
        <span>최대 {{ Math.round(view.scaleMax) }}{{ unit }}</span>
        <span>{{ view.lastTime }}</span>
      </div>
    </template>
  </div>
</template>
