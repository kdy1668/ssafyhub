import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { createPost } from "../api/posts";

export function useCreatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createPost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });
}
