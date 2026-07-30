<script setup>
import { ref, computed, watch, nextTick } from "vue";

// 배포 진행 로그를 깃헙 액션 로그처럼 표시한다.
// logs 항목: { time, status, message } (문자열도 허용).
const props = defineProps({
  logs: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  title: { type: String, default: "배포 로그" },
});

defineEmits(["refresh"]);

const autoScroll = ref(true);
const scrollBox = ref(null);

// 상태별 텍스트 색상 (깃헙 다크 팔레트). 대부분 라인은 밝은 회색, 상태 라벨만 강조.
const STATUS_COLOR = {
  PENDING: "#8b949e",
  BUILDING: "#d29922",
  DEPLOYING: "#58a6ff",
  RUNNING: "#3fb950",
  FAILED: "#f85149",
};

const lines = computed(() =>
  props.logs.map((log) => {
    if (typeof log === "string") return { time: "", status: "", message: log };
    return { time: log.time || "", status: log.status || "", message: log.message ?? "" };
  })
);

// 실패/성공 라인은 메시지 자체를 색으로 강조, 나머지는 밝은 회색.
function messageColor(status) {
  if (status === "FAILED") return "#f85149";
  if (status === "RUNNING") return "#3fb950";
  return "#c9d1d9";
}

// 새 로그가 추가되면 자동 스크롤
watch(
  () => props.logs.length,
  async () => {
    if (!autoScroll.value) return;
    await nextTick();
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight;
  }
);
</script>

<template>
  <div class="overflow-hidden rounded-lg border border-[#30363d] bg-[#0d1117] shadow-card">
    <!-- 헤더 (깃헙 액션 로그 헤더 느낌) -->
    <div class="flex items-center justify-between border-b border-[#30363d] bg-[#161b22] px-4 py-2.5">
      <div class="flex items-center gap-2">
        <span
          class="h-2.5 w-2.5 rounded-full"
          :class="loading ? 'animate-pulse bg-[#d29922]' : 'bg-[#3fb950]'"
        />
        <span class="font-mono text-sm font-semibold text-[#e6edf3]">{{ title }}</span>
        <span class="text-xs text-[#8b949e]">({{ lines.length }} lines)</span>
      </div>
      <div class="flex items-center gap-3">
        <label class="flex cursor-pointer items-center gap-1.5 text-xs text-[#8b949e]">
          <input type="checkbox" v-model="autoScroll" class="accent-[#58a6ff]" />
          자동 스크롤
        </label>
        <button
          type="button"
          class="rounded border border-[#30363d] bg-[#21262d] px-2.5 py-1 text-xs font-medium text-[#c9d1d9] hover:bg-[#30363d]"
          @click="$emit('refresh')"
        >
          ↻ 새로고침
        </button>
      </div>
    </div>

    <!-- 로그 본문 -->
    <div
      ref="scrollBox"
      class="h-96 overflow-y-auto bg-[#0d1117] py-2 font-mono text-xs leading-relaxed"
    >
      <!-- 로딩 -->
      <p v-if="loading && lines.length === 0" class="px-4 text-[#8b949e]">
        로그를 불러오는 중...
      </p>
      <!-- 빈 상태 -->
      <p v-else-if="lines.length === 0" class="px-4 text-[#6e7681]">
        아직 로그가 없습니다.
      </p>

      <!-- 로그 라인: 라인번호 + 타임스탬프 + 상태 + 메시지 -->
      <div v-else>
        <div
          v-for="(line, idx) in lines"
          :key="idx"
          class="group flex items-start gap-3 px-4 hover:bg-[#161b22]"
        >
          <!-- 라인 번호 -->
          <span class="w-8 shrink-0 select-none text-right text-[#484f58] group-hover:text-[#6e7681]">
            {{ idx + 1 }}
          </span>
          <!-- 타임스탬프 -->
          <span v-if="line.time" class="shrink-0 select-none text-[#6e7681]">{{ line.time }}</span>
          <!-- 상태 라벨 -->
          <span
            v-if="line.status"
            class="shrink-0 font-semibold"
            :style="{ color: STATUS_COLOR[line.status] || '#8b949e' }"
          >
            {{ line.status }}
          </span>
          <!-- 메시지 -->
          <span class="whitespace-pre-wrap break-all" :style="{ color: messageColor(line.status) }">
            {{ line.message }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>
