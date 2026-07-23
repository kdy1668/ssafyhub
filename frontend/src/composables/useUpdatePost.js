import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { updatePost } from "../api/posts";

export function useUpdatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }) => updatePost(id, payload),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["posts"] });
      queryClient.invalidateQueries({ queryKey: ["posts", String(variables.id)] });
    },
  });
}
