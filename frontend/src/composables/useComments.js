import { unref } from "vue";
import { useQuery, useMutation, useQueryClient } from "@tanstack/vue-query";
import { fetchComments, createComment, deleteComment } from "../api/comments";

export function useComments(postId) {
  return useQuery({
    queryKey: ["comments", postId],
    queryFn: () => fetchComments(unref(postId)),
    enabled: () => Boolean(unref(postId)),
  });
}

export function useCreateComment(postId) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload) => createComment(unref(postId), payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", postId] });
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });
}

export function useDeleteComment(postId) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (commentId) => deleteComment(unref(postId), commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", postId] });
      queryClient.invalidateQueries({ queryKey: ["posts"] });
    },
  });
}
