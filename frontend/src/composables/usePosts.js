import { useQuery } from "@tanstack/vue-query";
import { fetchPosts } from "../api/posts";

export function usePosts() {
  return useQuery({
    queryKey: ["posts"],
    queryFn: fetchPosts,
  });
}
