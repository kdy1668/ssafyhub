import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchDeploymentLogs } from "../api/deployments";

// 배포 로그 조회 (활성 시 3초 폴링으로 실시간 느낌)
export function useDeploymentLogs(deploymentId, enabled) {
  return useQuery({
    queryKey: computed(() => ["deployments", unref(deploymentId), "logs"]),
    queryFn: () => fetchDeploymentLogs(unref(deploymentId)),
    enabled: computed(() => Boolean(unref(deploymentId)) && Boolean(unref(enabled))),
    refetchInterval: () => (unref(enabled) ? 3000 : false),
  });
}
