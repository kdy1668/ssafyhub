import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchDeploymentResources } from "../api/deployments";

export function useDeploymentResources(deploymentId, enabled) {
  return useQuery({
    queryKey: computed(() => ["deployments", unref(deploymentId), "resources"]),
    queryFn: () => fetchDeploymentResources(unref(deploymentId)),
    enabled: computed(() => Boolean(unref(deploymentId)) && Boolean(unref(enabled))),
  });
}
