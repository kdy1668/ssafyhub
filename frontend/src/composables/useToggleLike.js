import { useMutation, useQueryClient } from "@tanstack/vue-query";
import { toggleLike } from "../api/posts";

export function useToggleLike() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (postId) => toggleLike(postId),
    onSuccess: () => {
      // ["posts"] 프리픽스 → 목록·상세 캐시 모두 갱신
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });
}
