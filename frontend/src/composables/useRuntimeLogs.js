import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchRuntimeLogs } from "../api/deployments";

// AWS 콘솔 로그 보기처럼 폴링 방식으로 "최근 로그" 스냅샷을 주기적으로 새로 받아온다(실시간 스트리밍 아님).
export function useRuntimeLogs(deploymentId, enabled, tail = 300) {
  return useQuery({
    queryKey: computed(() => ["deployments", unref(deploymentId), "runtime-logs", unref(tail)]),
    queryFn: () => fetchRuntimeLogs(unref(deploymentId), unref(tail)),
    enabled: computed(() => Boolean(unref(deploymentId)) && Boolean(unref(enabled))),
    refetchInterval: () => (unref(enabled) ? 5000 : false),
  });
}
