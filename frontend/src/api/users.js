import { useAuthStore } from "../stores/useAuthStore";

// 백엔드에 별도의 "현재 사용자 조회" 엔드포인트가 없으므로,
// 로그인 응답으로 저장된 member 정보를 현재 사용자로 사용한다. (비로그인 시 null)
export async function fetchCurrentUser() {
  return useAuthStore().member;
}
