<script setup>
import BaseButton from "../common/BaseButton.vue";

// 껍데기 UI: 실제 시작/중지/재시작 로직은 백엔드 팀에서 향후 연동 예정.
// 현재는 상태에 따라 버튼 활성/비활성만 처리하고 이벤트를 상위로 emit 한다.
const props = defineProps({
  // "RUNNING" | "DEPLOYED" | "STOPPED" | "FAILED" | "PENDING" | "BUILDING" | "DEPLOYING"
  status: { type: String, default: "" },
  // 각 액션별 진행 중 상태 (백엔드 연동 시 사용)
  loadingAction: { type: String, default: "" }, // "start" | "stop" | "restart"
  disabled: { type: Boolean, default: false },
});

defineEmits(["start", "stop", "restart"]);

// 실행 중으로 간주되는 상태
const RUNNING_STATES = ["RUNNING", "DEPLOYED"];
// 전이(작업) 중이라 제어를 막아야 하는 상태
const BUSY_STATES = ["PENDING", "BUILDING", "DEPLOYING"];

function isRunning() {
  return RUNNING_STATES.includes(props.status);
}
function isBusy() {
  return BUSY_STATES.includes(props.status) || props.disabled;
}
</script>

<template>
  <div class="flex items-center gap-2">
    <!-- 시작: 실행 중이 아닐 때만 활성 -->
    <BaseButton
      variant="primary"
      :disabled="isRunning() || isBusy()"
      :loading="loadingAction === 'start'"
      @click="$emit('start')"
    >
      ▶ 시작
    </BaseButton>

    <!-- 중지: 실행 중일 때만 활성 -->
    <BaseButton
      variant="danger"
      :disabled="!isRunning() || isBusy()"
      :loading="loadingAction === 'stop'"
      @click="$emit('stop')"
    >
      ■ 중지
    </BaseButton>

    <!-- 재시작: 실행 중일 때만 활성 -->
    <BaseButton
      variant="secondary"
      :disabled="!isRunning() || isBusy()"
      :loading="loadingAction === 'restart'"
      @click="$emit('restart')"
    >
      ↻ 재시작
    </BaseButton>
  </div>
</template>
