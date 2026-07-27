import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockUser } from "./mockData";

export async function fetchCurrentUser() {
  if (USE_MOCKS) {
    await mockDelay(400);
    // 비로그인 상태에서는 사용자 정보 없음(null)
    return localStorage.getItem("auth_token") ? mockUser : null;
  }
  const { data } = await apiClient.get("/api/users/me");
  return data;
}
