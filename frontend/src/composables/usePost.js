import { useQuery } from "@tanstack/vue-query";
import { computed, unref } from "vue";
import { fetchPost } from "../api/posts";

function isValidPostId(id) {
  return id !== undefined && id !== null && id !== "undefined" && id !== "null" && id !== "";
}

export function usePost(postId) {
  return useQuery({
    queryKey: computed(() => ["posts", unref(postId)]),
    queryFn: () => fetchPost(unref(postId)),
    enabled: computed(() => isValidPostId(unref(postId))),
  });
}
