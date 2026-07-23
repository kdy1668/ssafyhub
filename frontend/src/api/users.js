import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockUser } from "./mockData";

export async function fetchCurrentUser() {
  if (USE_MOCKS) {
    await mockDelay(400);
    return mockUser;
  }
  const { data } = await apiClient.get("/api/users/me");
  return data;
}
