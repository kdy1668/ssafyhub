<script setup>
import { computed, ref } from "vue";
import BaseBadge from "../common/BaseBadge.vue";
import BaseSkeleton from "../common/BaseSkeleton.vue";

// 배포 실패 원인 AI 진단 카드. 원인 → 이유 → 해결 절차 → 근거 순으로 보여준다.
//
// 근거가 된 로그를 이 컴포넌트가 직접 렌더링한다(응답의 analyzedLog). 별도 로그 API나
// LogViewer에 의존하지 않으므로, 진단이 가리키는 줄과 화면에 보이는 줄이 항상 일치한다.
const props = defineProps({
  diagnosis: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
});

defineEmits(["retry"]);

const CONFIDENCE_META = {
  HIGH: { tone: "info", label: "확신도 높음", note: null },
  MEDIUM: { tone: "warning", label: "확신도 보통", note: "로그만으로 단정하기 어려운 부분이 있습니다." },
  LOW: {
    tone: "warning",
    label: "확신도 낮음",
    note: "로그가 원인을 특정하기에 부족합니다. 참고용으로만 봐주세요.",
  },
};

const confidence = computed(
  () => CONFIDENCE_META[props.diagnosis?.confidence] ?? CONFIDENCE_META.MEDIUM
);

// 근거로 지목된 줄 번호. 발췌 범위를 벗어난 번호는 가리킬 줄이 없으므로 버린다.
const evidenceLineNumbers = computed(() => {
  const analyzed = props.diagnosis?.analyzedLog ?? [];
  const valid = new Set(analyzed.map((line) => line.lineNumber));
  return (props.diagnosis?.logLineNumbers ?? []).filter((number) => valid.has(number));
});

const evidenceSet = computed(() => new Set(evidenceLineNumbers.value));

// 기본적으로는 근거 줄만 보여주고, 필요하면 전체 발췌를 펼친다.
// 400줄까지 올 수 있어서 처음부터 다 펼치면 화면이 로그에 묻힌다.
const showFullLog = ref(false);

const visibleLog = computed(() => {
  const analyzed = props.diagnosis?.analyzedLog ?? [];
  if (showFullLog.value || evidenceSet.value.size === 0) return analyzed;
  return analyzed.filter((line) => evidenceSet.value.has(line.lineNumber));
});

const hasHiddenLines = computed(
  () => (props.diagnosis?.analyzedLog?.length ?? 0) > visibleLog.value.length
);
</script>

<template>
  <div
    v-if="loading || diagnosis || error"
    class="rounded-lg border border-border bg-surface shadow-card"
  >
    <!-- 헤더 -->
    <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border px-5 py-3">
      <div class="flex items-center gap-2">
        <span class="text-sm font-bold text-ink-900">실패 원인 분석</span>
        <BaseBadge tone="neutral">AI 생성</BaseBadge>
      </div>
      <BaseBadge v-if="diagnosis" :tone="confidence.tone">{{ confidence.label }}</BaseBadge>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="flex flex-col gap-3 px-5 py-4">
      <p class="text-sm text-ink-500">실패 원인을 분석하고 있습니다...</p>
      <BaseSkeleton class="h-4 w-3/4" />
      <BaseSkeleton class="h-4 w-full" />
      <BaseSkeleton class="h-4 w-2/3" />
    </div>

    <!-- 실패: 카드만 접고 아래 로그 뷰어는 그대로 쓸 수 있어야 한다 -->
    <div v-else-if="error" class="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
      <p class="text-sm text-ink-600">
        원인 분석에 실패했습니다. 아래 배포 로그는 그대로 확인할 수 있습니다.
      </p>
      <button
        type="button"
        class="rounded-md border border-border bg-surface px-3 py-1.5 text-xs font-medium text-ink-700 hover:bg-black/5"
        @click="$emit('retry')"
      >
        다시 분석
      </button>
    </div>

    <!-- 진단 결과 -->
    <div v-else class="flex flex-col gap-4 px-5 py-4">
      <p class="text-base font-semibold leading-relaxed text-ink-900">{{ diagnosis.summary }}</p>

      <p class="whitespace-pre-line text-sm leading-relaxed text-ink-700">{{ diagnosis.detail }}</p>

      <p v-if="confidence.note" class="rounded-md bg-accent-light px-3 py-2 text-xs text-accent">
        {{ confidence.note }}
      </p>

      <!-- 해결 절차 -->
      <div v-if="diagnosis.actions?.length" class="flex flex-col gap-2">
        <p class="text-sm font-bold text-ink-900">이렇게 해보세요</p>
        <ol class="flex flex-col gap-2">
          <li
            v-for="(action, index) in diagnosis.actions"
            :key="index"
            class="flex gap-3 rounded-md bg-primary-light/60 px-3 py-2.5"
          >
            <span
              class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-[11px] font-bold text-white"
            >
              {{ index + 1 }}
            </span>
            <span class="flex flex-col gap-0.5">
              <span class="text-sm font-semibold text-ink-900">{{ action.title }}</span>
              <span class="text-sm leading-relaxed text-ink-700">{{ action.description }}</span>
            </span>
          </li>
        </ol>
      </div>

      <!-- 근거 로그 -->
      <div v-if="diagnosis.analyzedLog?.length" class="flex flex-col gap-2">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="text-sm font-bold text-ink-900">
            판단 근거
            <span v-if="evidenceLineNumbers.length" class="font-normal text-ink-500">
              ({{ evidenceLineNumbers.join(", ") }}번째 줄)
            </span>
          </p>
          <button
            v-if="hasHiddenLines || showFullLog"
            type="button"
            class="text-xs font-medium text-primary hover:underline"
            @click="showFullLog = !showFullLog"
          >
            {{ showFullLog ? "근거 줄만 보기" : "분석한 로그 전체 보기" }}
          </button>
        </div>

        <p v-if="diagnosis.omittedLeadingLines > 0" class="text-xs text-ink-500">
          로그가 길어 앞부분 {{ diagnosis.omittedLeadingLines }}줄은 분석에서 제외했습니다.
        </p>

        <div class="max-h-64 overflow-y-auto rounded-md bg-ink-900 px-3 py-2 font-mono text-xs leading-relaxed">
          <div
            v-for="line in visibleLog"
            :key="line.lineNumber"
            class="flex gap-3"
            :class="evidenceSet.has(line.lineNumber) ? 'bg-white/10' : ''"
          >
            <span class="w-8 shrink-0 select-none text-right text-white/40">{{ line.lineNumber }}</span>
            <span
              class="whitespace-pre-wrap break-all"
              :class="evidenceSet.has(line.lineNumber) ? 'text-white' : 'text-white/60'"
            >{{ line.message }}</span>
          </div>
        </div>
      </div>

      <p class="text-xs text-ink-300">
        AI가 배포 로그를 읽고 생성한 분석입니다. 실제 원인과 다를 수 있으니 근거 로그를 함께 확인해 주세요.
      </p>
    </div>
  </div>
</template>
