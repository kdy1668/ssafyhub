import { useQuery } from "@tanstack/vue-query";
import { fetchCurrentUser } from "../api/users";

export function useCurrentUser() {
  return useQuery({
    queryKey: ["users", "me"],
    queryFn: fetchCurrentUser,
    // 로그인/로그아웃 직후 최신 member를 반영하도록 마운트 시 항상 최신화
    staleTime: 0,
  });
}
