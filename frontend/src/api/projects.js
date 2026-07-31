import { apiClient } from "./client";

// ⚠️ 백엔드 미구현 — POST /api/projects 는 존재하지 않는다(현재 호출하면 404).
// 실제 배포 생성은 api/deployments.js 의 createDeployment(POST /api/v1/deployments)를 쓴다.
export async function createProject(payload) {
  const { data } = await apiClient.post("/api/projects", payload);
  return data;
}
