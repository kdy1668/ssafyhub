<script setup>
import BaseModal from "../common/BaseModal.vue";
import ResourceDashboard from "./ResourceDashboard.vue";

const props = defineProps({
  open: Boolean,
  deploymentId: String,
  projectName: String,
});
defineEmits(["close"]);
</script>

<template>
  <BaseModal :open="open" :title="`${projectName} 리소스 사용량`" @close="$emit('close')">
    <ResourceDashboard :deployment-id="props.deploymentId" :enabled="props.open" />
  </BaseModal>
</template>
