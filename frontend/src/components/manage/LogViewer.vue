<script setup>
import { ref, computed, watch, nextTick } from "vue";

// 껍데기 UI: 실제 로그 스트리밍/조회는 백엔드 팀에서 향후 연동 예정.
// logs prop 으로 로그 라인 배열을 받아 터미널 형태로 표시한다.
const props = defineProps({
  // 각 항목: string 이거나 { time?, level?, message } 형태 모두 허용
  logs: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  title: { type: String, default: "실시간 로그" },
});

defineEmits(["refresh"]);

const autoScroll = ref(true);
const scrollBox = ref(null);

const lines = computed(() =>
  props.logs.map((log) => {
    if (typeof log === "string") return { message: log };
    return log;
  })
);

const LEVEL_CLASSES = {
  ERROR: "text-red-400",
  WARN: "text-yellow-400",
  INFO: "text-sky-300",
  DEBUG: "text-ink-400",
};

// 새 로그가 추가되면 자동 스크롤
watch(
  () => props.logs.length,
  async () => {
    if (!autoScroll.value) return;
    await nextTick();
    if (scrollBox.value) {
      scrollBox.value.scrollTop = scrollBox.value.scrollHeight;
    }
  }
);
</script>

<template>
  <div class="rounded-lg border border-border bg-surface shadow-card overflow-hidden">
    <!-- 헤더 -->
    <div class="flex items-center justify-between border-b border-border px-4 py-3">
      <div class="flex items-center gap-2">
        <span class="text-sm font-bold text-ink-900">{{ title }}</span>
        <span
          class="h-2 w-2 rounded-full"
          :class="loading ? 'bg-yellow-400 animate-pulse' : 'bg-green-500'"
        />
      </div>
      <div class="flex items-center gap-3">
        <label class="flex cursor-pointer items-center gap-1.5 text-xs text-ink-600">
          <input type="checkbox" v-model="autoScroll" class="accent-primary" />
          자동 스크롤
        </label>
        <button
          type="button"
          class="rounded border border-border bg-surface px-2.5 py-1 text-xs font-medium text-ink-700 hover:bg-ink-100"
          @click="$emit('refresh')"
        >
          ↻ 새로고침
        </button>
      </div>
    </div>

    <!-- 로그 본문 (터미널 스타일) -->
    <div
      ref="scrollBox"
      class="h-80 overflow-y-auto bg-[#0d1117] px-4 py-3 font-mono text-xs leading-relaxed"
    >
      <!-- 로딩 -->
      <p v-if="loading && lines.length === 0" class="text-ink-400">
        로그를 불러오는 중...
      </p>

      <!-- 빈 상태 -->
      <p v-else-if="lines.length === 0" class="text-ink-500">
        표시할 로그가 없습니다. (백엔드 로그 연동 예정)
      </p>

      <!-- 로그 라인 -->
      <div v-else class="flex flex-col gap-0.5">
        <div v-for="(line, idx) in lines" :key="idx" class="flex gap-2 whitespace-pre-wrap break-all">
          <span v-if="line.time" class="shrink-0 text-ink-500">{{ line.time }}</span>
          <span
            v-if="line.level"
            class="shrink-0 font-semibold"
            :class="LEVEL_CLASSES[line.level] || 'text-ink-300'"
          >
            [{{ line.level }}]
          </span>
          <span class="text-ink-100">{{ line.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
