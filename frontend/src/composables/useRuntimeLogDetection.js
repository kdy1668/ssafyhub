import { useMutation } from "@tanstack/vue-query";
import { onUnmounted, ref, unref } from "vue";
import { detectRuntimeLogAnomalies } from "../api/ai";

/**
 * 런타임 로그 이상 감지. **버튼 클릭으로만 실행된다.**
 *
 * 폴링에 연결하면 안 된다. 런타임 로그 화면은 5초마다 로그를 새로 받아오는데, 여기에
 * 감지를 얹으면 사용자가 화면을 열어둔 것만으로 시간당 720회 AI 호출이 발생한다.
 * 그래서 mutation으로 두고, 서버 쿨다운과 별개로 클라이언트에서도 버튼을 잠근다.
 *
 * 남은 시간은 에러 응답에 들어오지 않는다(공통 예외 핸들러가 데이터를 담지 않는다).
 * 대신 /ai/availability가 알려준 간격으로 여기서 카운트다운한다.
 *
 * @param deploymentId    배포 id
 * @param cooldownSeconds 쿨다운 간격(ref) - useAiConfig()에서 받은 값
 */
export function useRuntimeLogDetection(deploymentId, cooldownSeconds) {
  const cooldownRemaining = ref(0);
  let timer = null;

  function stopTimer() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  function startCooldown() {
    stopTimer();
    cooldownRemaining.value = unref(cooldownSeconds) ?? 30;
    timer = setInterval(() => {
      cooldownRemaining.value -= 1;
      if (cooldownRemaining.value <= 0) {
        cooldownRemaining.value = 0;
        stopTimer();
      }
    }, 1000);
  }

  onUnmounted(stopTimer);

  const mutation = useMutation({
    mutationFn: () => detectRuntimeLogAnomalies(unref(deploymentId)),
    onSuccess: startCooldown,
    onError: (error) => {
      // 서버가 쿨다운으로 거부한 경우(AI_003)에도 카운트다운을 걸어 버튼을 잠근다.
      // 서버 쪽이 우리보다 늦게 풀리는 상황(앱 재시작 등)에 대비한 방어다.
      if (error?.code === "AI_003") {
        startCooldown();
      }
    },
  });

  return { ...mutation, cooldownRemaining };
}
