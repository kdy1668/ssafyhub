import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { deletePost } from "../api/posts";

export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deletePost,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });
}
