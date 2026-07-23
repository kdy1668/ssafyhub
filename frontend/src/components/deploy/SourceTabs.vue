<script setup>
import BaseInput from "../common/BaseInput.vue";
import { usePresignedUpload } from "../../composables/usePresignedUpload";

defineProps({
  sourceType: String,
  githubUrl: String,
  sourceFileKey: String,
  sourceFileName: String,
  errors: { type: Object, default: () => ({}) },
});

const emit = defineEmits([
  "update:sourceType",
  "update:githubUrl",
  "update:sourceFileKey",
  "update:sourceFileName",
]);

const TABS = [
  { value: "github", label: "GitHub URL" },
  { value: "file", label: "파일 업로드" },
];

const presignedUpload = usePresignedUpload();

async function handleFileChange(event) {
  const file = event.target.files?.[0];
  if (!file) return;

  try {
    const result = await presignedUpload.mutateAsync(file);
    emit("update:sourceFileKey", result.key);
    emit("update:sourceFileName", result.fileName);
  } catch {
    emit("update:sourceFileKey", "");
  }
}
</script>

<template>
  <div class="flex flex-col gap-3">
    <div class="flex gap-1 rounded-md bg-black/5 p-1">
      <button
        v-for="tab in TABS"
        :key="tab.value"
        type="button"
        :class="[
          'flex-1 rounded px-3 py-1.5 text-sm font-medium transition-colors',
          sourceType === tab.value ? 'bg-surface text-primary shadow-card' : 'text-ink-500 hover:text-ink-900',
        ]"
        @click="$emit('update:sourceType', tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <BaseInput
      v-if="sourceType === 'github'"
      id="githubUrl"
      label="GitHub 저장소 URL"
      placeholder="https://github.com/org/repo"
      :model-value="githubUrl"
      :error="errors.githubUrl"
      @update:model-value="$emit('update:githubUrl', $event)"
    />

    <div v-else class="flex flex-col gap-1">
      <label class="text-sm font-medium text-ink-700">소스 파일</label>
      <input type="file" class="text-sm" @change="handleFileChange" />
      <span v-if="presignedUpload.isPending.value" class="text-xs text-ink-500">
        S3에 업로드 중...
      </span>
      <span v-else-if="sourceFileKey" class="text-xs text-success">
        업로드 완료: {{ sourceFileKey }}
      </span>
      <span v-if="errors.sourceFileKey" class="text-xs text-danger">{{ errors.sourceFileKey }}</span>
    </div>
  </div>
</template>
