<script setup>
import { computed } from "vue";
import BaseButton from "../common/BaseButton.vue";
import AppIcon from "../common/AppIcon.vue";

const props = defineProps({
  // useCreateDeployment().uploads (reactive: { backend, frontend })
  uploads: { type: Object, required: true },
  // idle | preparing | uploading | creating | done | error
  phase: { type: String, default: "idle" },
});

defineEmits(["retry"]);

const items = computed(() => [props.uploads.backend, props.uploads.frontend]);

const hasError = computed(() => props.phase === "error");
const isDone = computed(() => props.phase === "done");

const PHASE_LABEL = {
  preparing: "업로드 URL을 준비하고 있습니다...",
  uploading: "파일을 업로드하고 있습니다...",
  creating: "배포 요청을 생성하고 있습니다...",
  done: "업로드 및 배포 요청이 완료되었습니다.",
  error: "일부 파일 업로드에 실패했습니다.",
};

function barClass(status) {
  if (status === "error") return "bg-danger";
  if (status === "success") return "bg-success";
  return "bg-primary";
}
</script>

<template>
  <div class="flex flex-col gap-4 rounded-md border border-border bg-background p-4">
    <!-- 단계 안내 -->
    <div class="flex items-center gap-2">
      <span
        v-if="phase === 'preparing' || phase === 'uploading' || phase === 'creating'"
        class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-primary border-t-transparent"
      />
      <AppIcon v-else-if="isDone" name="check" class="h-4 w-4 text-success" />
      <AppIcon v-else-if="hasError" name="alert" class="h-4 w-4 text-danger" />
      <p
        class="text-sm font-medium"
        :class="hasError ? 'text-danger' : isDone ? 'text-success' : 'text-ink-700'"
      >
        {{ PHASE_LABEL[phase] || "" }}
      </p>
    </div>

    <!-- 파일별 진행률 -->
    <div v-for="item in items" :key="item.label" class="flex flex-col gap-1.5">
      <div class="flex items-center justify-between text-xs">
        <span class="inline-flex items-center gap-1.5 font-medium text-ink-700">
          <AppIcon v-if="item.status === 'success'" name="check" class="h-3.5 w-3.5 text-success" />
          <AppIcon v-else-if="item.status === 'error'" name="x" class="h-3.5 w-3.5 text-danger" />
          <span
            v-else-if="item.status === 'uploading'"
            class="h-3 w-3 animate-spin rounded-full border-2 border-primary border-t-transparent"
          />
          <span v-else class="h-1.5 w-1.5 rounded-full bg-ink-300" />
          {{ item.label }}
          <span class="ml-1 text-ink-500">{{ item.name }}</span>
        </span>
        <span
          class="tabular-nums"
          :class="item.status === 'error' ? 'text-danger' : 'text-ink-600'"
        >
          {{ item.status === "error" ? "실패" : `${item.progress}%` }}
        </span>
      </div>

      <div class="h-2 w-full overflow-hidden rounded-full bg-black/10">
        <div
          class="h-2 rounded-full transition-all duration-200"
          :class="barClass(item.status)"
          :style="{ width: `${item.progress}%` }"
        />
      </div>

      <!-- 파일별 에러 메시지 -->
      <p v-if="item.status === 'error' && item.error" class="text-xs text-danger">
        {{ item.error }}
      </p>
    </div>

    <!-- 실패 시 재시도 영역 -->
    <div
      v-if="hasError"
      class="flex flex-col gap-3 rounded-md border border-danger/40 bg-danger-light/40 p-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <p class="text-xs text-danger">
        업로드에 실패했습니다. 네트워크 상태를 확인한 뒤 다시 시도해 주세요.
      </p>
      <BaseButton variant="danger" class="shrink-0" @click="$emit('retry')">
        ↻ 다시 시도
      </BaseButton>
    </div>
  </div>
</template>
