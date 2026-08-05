<script setup>
import { computed } from "vue";
import BaseBadge from "../common/BaseBadge.vue";

// 런타임 로그 이상 감지 패널. 버튼을 눌렀을 때만 분석한다(폴링에 연결하지 않는다).
const props = defineProps({
  detection: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  error: { type: Object, default: null },
  cooldownRemaining: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false },
});

defineEmits(["detect"]);

const HEALTH_META = {
  HEALTHY: { tone: "success", label: "이상 없음" },
  WARNING: { tone: "warning", label: "주의 필요" },
  ERROR: { tone: "danger", label: "문제 발생" },
  UNKNOWN: { tone: "neutral", label: "판단 불가" },
};

const SEVERITY_META = {
  ERROR: { tone: "danger", label: "오류" },
  WARNING: { tone: "warning", label: "주의" },
  INFO: { tone: "neutral", label: "참고" },
};

const health = computed(() => HEALTH_META[props.detection?.health] ?? HEALTH_META.UNKNOWN);

const buttonDisabled = computed(
  () => props.disabled || props.loading || props.cooldownRemaining > 0
);

const buttonLabel = computed(() => {
  if (props.loading) return "확인 중...";
  if (props.cooldownRemaining > 0) return `${props.cooldownRemaining}초 후 다시 확인`;
  return props.detection ? "다시 확인" : "이상 징후 확인";
});
</script>

<template>
  <div class="rounded-lg border border-border bg-surface shadow-card">
    <!-- 헤더 + 실행 버튼 -->
    <div class="flex flex-wrap items-center justify-between gap-2 border-b border-border px-5 py-3">
      <div class="flex items-center gap-2">
        <span class="text-sm font-bold text-ink-900">이상 징후 확인</span>
        <BaseBadge tone="neutral">AI 생성</BaseBadge>
        <BaseBadge v-if="detection" :tone="health.tone">{{ health.label }}</BaseBadge>
      </div>
      <button
        type="button"
        :disabled="buttonDisabled"
        class="rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-white hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
        @click="$emit('detect')"
      >
        {{ buttonLabel }}
      </button>
    </div>

    <div class="px-5 py-4">
      <!-- 아직 실행 전 -->
      <p v-if="!detection && !loading && !error" class="text-sm text-ink-500">
        지금 앱 로그에 문제될 징후가 있는지 AI가 확인해 드립니다. 버튼을 누르면 최근 로그를
        분석합니다. 자동으로 실행되지는 않습니다.
      </p>

      <p v-else-if="loading" class="text-sm text-ink-500">최근 로그를 확인하고 있습니다...</p>

      <p v-else-if="error" class="text-sm text-ink-600">
        {{
          error.code === "AI_003"
            ? "방금 확인했습니다. 잠시 후 다시 시도해 주세요."
            : "로그 확인에 실패했습니다. 아래 로그는 그대로 볼 수 있습니다."
        }}
      </p>

      <!-- 결과 -->
      <div v-else class="flex flex-col gap-4">
        <p class="text-sm leading-relaxed text-ink-900">{{ detection.summary }}</p>

        <!-- 이상 없음을 명시적으로 알려준다. 빈 화면이면 기능이 안 돈 것으로 오해한다. -->
        <p
          v-if="detection.health === 'HEALTHY'"
          class="rounded-md bg-success-light px-3 py-2 text-sm text-success"
        >
          최근 {{ detection.analyzedLines }}줄에서 특별한 문제가 보이지 않습니다.
        </p>

        <div v-if="detection.findings?.length" class="flex flex-col gap-2">
          <div
            v-for="(finding, index) in detection.findings"
            :key="index"
            class="flex flex-col gap-1.5 rounded-md border border-border px-3 py-2.5"
          >
            <div class="flex flex-wrap items-center gap-2">
              <BaseBadge :tone="(SEVERITY_META[finding.severity] ?? SEVERITY_META.INFO).tone">
                {{ (SEVERITY_META[finding.severity] ?? SEVERITY_META.INFO).label }}
              </BaseBadge>
              <span class="text-sm font-semibold text-ink-900">{{ finding.title }}</span>
            </div>
            <p class="text-sm leading-relaxed text-ink-700">{{ finding.detail }}</p>
            <p v-if="finding.suggestion" class="text-sm leading-relaxed text-primary">
              → {{ finding.suggestion }}
            </p>
          </div>
        </div>

        <div v-if="detection.nextSteps?.length" class="flex flex-col gap-1">
          <p class="text-sm font-bold text-ink-900">확인해 볼 것</p>
          <ul class="flex flex-col gap-1">
            <li
              v-for="(step, index) in detection.nextSteps"
              :key="index"
              class="text-sm leading-relaxed text-ink-700"
            >
              · {{ step }}
            </li>
          </ul>
        </div>

        <p class="text-xs text-ink-300">
          최근 {{ detection.analyzedLines }}줄만 본 결과입니다. 전체 이력이 아니므로 참고용으로
          봐주세요.
        </p>
      </div>
    </div>
  </div>
</template>
