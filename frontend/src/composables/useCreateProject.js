import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { createProject } from "../api/projects";

export function useCreateProject() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deployments"] });
    },
  });
}
