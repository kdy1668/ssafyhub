<script setup>
import BaseBadge from "../common/BaseBadge.vue";

defineProps({
  kind: { type: String, required: true }, // "server" | "deploy"
  status: { type: String, required: true },
});

const SERVER_STATUS_MAP = {
  RUNNING: { label: "실행 중", tone: "success" },
  STOPPED: { label: "중지됨", tone: "neutral" },
};

const DEPLOY_STATUS_MAP = {
  DEPLOYED: { label: "배포 완료", tone: "success" },
  RUNNING: { label: "정상 기동", tone: "success" },
  DEPLOYING: { label: "배포중", tone: "warning" },
  PENDING: { label: "대기 중", tone: "warning" },
  BUILDING: { label: "빌드 중", tone: "warning" },
  FAILED: { label: "배포 실패", tone: "danger" },
};

function resolve(kind, status) {
  const map = kind === "server" ? SERVER_STATUS_MAP : DEPLOY_STATUS_MAP;
  return map[status] || { label: status, tone: "neutral" };
}
</script>

<template>
  <BaseBadge :tone="resolve(kind, status).tone">{{ resolve(kind, status).label }}</BaseBadge>
</template>
