import { useQuery } from "@tanstack/vue-query";
import { computed, toValue } from "vue";
import { fetchPublicShowcases, fetchShowcase } from "../api/showcase";

export function usePublicShowcases(page, size = 12) {
  return useQuery({
    queryKey: computed(() => ["showcase", "public", toValue(page), toValue(size)]),
    queryFn: () => fetchPublicShowcases(toValue(page), toValue(size)),
    placeholderData: (previousData) => previousData,
  });
}

export function usePublicShowcase(id) {
  return useQuery({
    queryKey: computed(() => ["showcase", toValue(id)]),
    queryFn: () => fetchShowcase(toValue(id)),
    enabled: computed(() => Boolean(toValue(id))),
  });
}
