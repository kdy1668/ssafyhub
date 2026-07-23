import { apiClient, USE_MOCKS, mockDelay } from "./client";

export async function createProject(payload) {
  if (USE_MOCKS) {
    await mockDelay(1200);
    if (!payload.projectName) {
      throw new Error("프로젝트 이름은 필수입니다.");
    }
    return {
      id: `dep-${Math.floor(Math.random() * 9000 + 1000)}`,
      projectName: payload.projectName,
      deployStatus: "DEPLOYING",
      createdAt: new Date().toISOString(),
    };
  }
  const { data } = await apiClient.post("/api/projects", payload);
  return data;
}
