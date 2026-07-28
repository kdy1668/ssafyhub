<script setup>
import { computed } from "vue";
import BaseModal from "../common/BaseModal.vue";
import BaseButton from "../common/BaseButton.vue";

const props = defineProps({
  open: Boolean,
  values: { type: Object, required: true },
});
defineEmits(["close"]);

const envVars = computed(() => (props.values.envVars || []).filter((v) => v.key));
</script>

<template>
  <BaseModal :open="open" title="배포 신청 미리보기" @close="$emit('close')">
    <dl class="flex flex-col gap-3 text-sm">
      <div class="flex justify-between border-b border-border pb-2">
        <dt class="font-medium text-ink-700">프로젝트 이름</dt>
        <dd class="text-ink-900">{{ values.projectName || "-" }}</dd>
      </div>
      <div class="flex justify-between border-b border-border pb-2">
        <dt class="font-medium text-ink-700">소스</dt>
        <dd class="text-ink-900">
          {{ values.sourceType === "github" ? values.githubUrl || "-" : values.sourceFileName || "-" }}
        </dd>
      </div>
      <div class="flex justify-between border-b border-border pb-2">
        <dt class="font-medium text-ink-700">언어/프레임워크</dt>
        <dd class="text-ink-900">{{ values.framework || "-" }}</dd>
      </div>
      <div class="flex justify-between border-b border-border pb-2">
        <dt class="font-medium text-ink-700">DB 비밀번호</dt>
        <dd class="text-ink-900">{{ values.dbPassword ? "•".repeat(8) : "-" }}</dd>
      </div>
      <div>
        <dt class="mb-1 font-medium text-ink-700">환경변수</dt>
        <dd>
          <span v-if="envVars.length === 0" class="text-ink-500">없음</span>
          <ul v-else class="flex flex-col gap-1">
            <li
              v-for="(v, i) in envVars"
              :key="i"
              class="rounded bg-black/5 px-2 py-1 font-mono text-xs"
            >
              {{ v.key }}={{ v.value }}
            </li>
          </ul>
        </dd>
      </div>
    </dl>
    <div class="mt-6 flex justify-end">
      <BaseButton variant="secondary" @click="$emit('close')">닫기</BaseButton>
    </div>
  </BaseModal>
</template>
