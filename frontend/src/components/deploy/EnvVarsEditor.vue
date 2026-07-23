<script setup>
import BaseInput from "../common/BaseInput.vue";
import BaseButton from "../common/BaseButton.vue";

const props = defineProps({
  modelValue: { type: Array, required: true },
});
const emit = defineEmits(["update:modelValue"]);

function addRow() {
  emit("update:modelValue", [...props.modelValue, { key: "", value: "" }]);
}

function removeRow(index) {
  emit(
    "update:modelValue",
    props.modelValue.filter((_, i) => i !== index)
  );
}

function updateRow(index, field, value) {
  const next = props.modelValue.map((row, i) =>
    i === index ? { ...row, [field]: value } : row
  );
  emit("update:modelValue", next);
}
</script>

<template>
  <div class="flex flex-col gap-2">
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium text-ink-700">환경변수</label>
      <BaseButton variant="secondary" @click="addRow">+ 추가</BaseButton>
    </div>

    <p v-if="modelValue.length === 0" class="text-xs text-ink-500">
      등록된 환경변수가 없습니다.
    </p>

    <div v-for="(row, index) in modelValue" :key="index" class="flex items-start gap-2">
      <BaseInput
        placeholder="KEY"
        class="flex-1"
        :model-value="row.key"
        @update:model-value="updateRow(index, 'key', $event)"
      />
      <BaseInput
        placeholder="VALUE"
        class="flex-1"
        :model-value="row.value"
        @update:model-value="updateRow(index, 'value', $event)"
      />
      <BaseButton variant="ghost" class="mt-0.5" @click="removeRow(index)">삭제</BaseButton>
    </div>
  </div>
</template>
