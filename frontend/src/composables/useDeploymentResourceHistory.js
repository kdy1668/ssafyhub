import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchDeploymentResourceHistory } from "../api/deployments";

// 리소스 시계열(그래프). 열려 있을 때 30초마다 갱신해 그래프가 실시간처럼 확장된다.
export function useDeploymentResourceHistory(deploymentId, enabled, minutes = 30) {
  return useQuery({
    queryKey: computed(() => [
      "deployments",
      unref(deploymentId),
      "resources",
      "history",
      unref(minutes),
    ]),
    queryFn: () => fetchDeploymentResourceHistory(unref(deploymentId), unref(minutes)),
    enabled: computed(() => Boolean(unref(deploymentId)) && Boolean(unref(enabled))),
    refetchInterval: () => (unref(enabled) ? 30000 : false),
  });
}
