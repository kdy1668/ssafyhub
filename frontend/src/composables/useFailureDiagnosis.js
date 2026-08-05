import { useQuery, useQueryClient } from "@tanstack/vue-query";
import { computed, unref, watch } from "vue";
import { createFailureDiagnosis } from "../api/ai";

/**
 * 배포 실패 원인 진단. 상태가 FAILED일 때 자동으로 1회 요청한다.
 *
 * POST인데 useQuery를 쓰는 이유: 서버가 (배포, 실패로그) 단위로 캐시해 멱등하게 동작하므로
 * 조회에 가깝다. 화면 진입 시 자동 요청과 결과 캐싱이 필요해서 mutation보다 맞다.
 *
 * 폴링하지 않는다. 진단은 실패 1건에 대해 한 번 만들어지면 더 바뀌지 않는다.
 *
 * @param deploymentId 배포 id
 * @param status       현재 배포 상태(ref). FAILED로 새로 전이될 때 새 진단을 받아온다.
 * @param enabled      AI 기능 활성 여부(ref)
 */
export function useFailureDiagnosis(deploymentId, status, enabled) {
  const queryClient = useQueryClient();
  const queryKey = computed(() => ["deployments", unref(deploymentId), "diagnosis"]);

  // 재배포 후 다시 실패하면 이전 진단은 더 이상 맞지 않다. FAILED로 "새로 전이"될 때만
  // 무효화한다 - FAILED가 계속 유지되는 동안 무효화하면 폴링과 다름없어진다.
  watch(
    () => unref(status),
    (next, previous) => {
      if (next === "FAILED" && previous && previous !== "FAILED") {
        queryClient.invalidateQueries({ queryKey: queryKey.value });
      }
    }
  );

  return useQuery({
    queryKey,
    queryFn: () => createFailureDiagnosis(unref(deploymentId)),
    enabled: computed(
      () =>
        Boolean(unref(enabled)) &&
        Boolean(unref(deploymentId)) &&
        unref(status) === "FAILED"
    ),
    refetchInterval: false,
    refetchOnWindowFocus: false,
    staleTime: Infinity,
    retry: false,
  });
}
