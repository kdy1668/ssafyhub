<script setup>
defineProps({
  label: String,
  error: String,
  modelValue: String,
  id: String,
  options: { type: Array, required: true },
});
defineEmits(["update:modelValue"]);
</script>

<template>
  <div class="flex flex-col gap-1">
    <label v-if="label" :for="id" class="text-sm font-medium text-ink-700">{{ label }}</label>
    <select
      :id="id"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)"
      :class="[
        'rounded-md border px-3 py-2 text-sm text-ink-900 outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary',
        error ? 'border-danger' : 'border-border',
      ]"
    >
      <option value="">선택하세요</option>
      <option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
    </select>
    <span v-if="error" class="text-xs text-danger">{{ error }}</span>
  </div>
</template>
