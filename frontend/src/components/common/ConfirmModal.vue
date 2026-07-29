<script setup>
import BaseModal from "./BaseModal.vue";
import BaseButton from "./BaseButton.vue";

// 범용 확인(Yes/No) 모달.
// 파괴적/중요한 액션 전에 사용자 확인을 받는다.
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: "확인" },
  message: { type: String, default: "" },
  confirmText: { type: String, default: "확인" },
  cancelText: { type: String, default: "취소" },
  // 확인 버튼 스타일: "primary" | "danger"
  confirmVariant: { type: String, default: "primary" },
  loading: { type: Boolean, default: false },
});

defineEmits(["confirm", "cancel"]);
</script>

<template>
  <BaseModal :open="open" :title="title" width="max-w-md" @close="$emit('cancel')">
    <p class="text-sm leading-relaxed text-ink-700">
      <slot>{{ message }}</slot>
    </p>

    <template #footer>
      <BaseButton variant="ghost" :disabled="loading" @click="$emit('cancel')">
        {{ cancelText }}
      </BaseButton>
      <BaseButton :variant="confirmVariant" :loading="loading" @click="$emit('confirm')">
        {{ confirmText }}
      </BaseButton>
    </template>
  </BaseModal>
</template>
