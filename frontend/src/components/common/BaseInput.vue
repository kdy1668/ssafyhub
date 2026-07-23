<script setup>
defineProps({
  label: String,
  error: String,
  modelValue: [String, Number],
  type: { type: String, default: "text" },
  id: String,
  placeholder: String,
});
defineEmits(["update:modelValue"]);
</script>

<template>
  <div class="flex flex-col gap-1">
    <label v-if="label" :for="id" class="text-sm font-medium text-ink-700">{{ label }}</label>
    <input
      :id="id"
      :type="type"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :class="[
        'rounded-md border px-3 py-2 text-sm text-ink-900 outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary',
        error ? 'border-danger' : 'border-border',
      ]"
    />
    <span v-if="error" class="text-xs text-danger">{{ error }}</span>
  </div>
</template>
