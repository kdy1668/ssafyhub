import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchDeploymentResources } from "../api/deployments";

export function useDeploymentResources(deploymentId, enabled) {
  return useQuery({
    queryKey: computed(() => ["deployments", unref(deploymentId), "resources"]),
    queryFn: () => fetchDeploymentResources(unref(deploymentId)),
    enabled: computed(() => Boolean(unref(deploymentId)) && Boolean(unref(enabled))),
    // 활성 시 3초마다 갱신하여 실시간 메트릭 느낌 제공
    refetchInterval: () => (unref(enabled) ? 3000 : false),
  });
}
