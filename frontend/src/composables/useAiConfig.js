import { useQuery } from "@tanstack/vue-query";
import { computed } from "vue";
import { fetchAiConfig } from "../api/ai";

/**
 * AI 기능 활성 여부와 설정값. 서버 기동 후에는 바뀌지 않으므로 한 번만 받아 계속 쓴다.
 *
 * 실패해도 재시도하지 않는다. AI는 부가 기능이라 조회가 안 되면 그냥 꺼진 것으로 취급하는 게
 * 맞고, 재시도가 화면 로딩을 붙잡을 이유가 없다.
 */
export function useAiConfig() {
  const query = useQuery({
    queryKey: ["ai", "config"],
    queryFn: fetchAiConfig,
    staleTime: Infinity,
    refetchInterval: false,
    refetchOnWindowFocus: false,
    retry: false,
  });

  // 서버는 runtimeLogCooldownSeconds도 함께 주지만 쓰지 않는다. 런타임 로그 이상 감지
  // 기능을 화면에서 걷어냈고, 그 값은 그 기능 전용이다.
  return {
    ...query,
    aiEnabled: computed(() => query.data.value?.enabled === true),
  };
}
