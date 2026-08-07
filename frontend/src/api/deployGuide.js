import { apiClient } from "./client";

// 온보딩 배포 가이드 생성. payload: { projectName, fileTree, keyFiles:[{path,content}] }
// 응답: { guide, markdown, model }
export async function generateDeployGuide(payload) {
  const { data } = await apiClient.post("/api/v1/deploy-guide", payload);
  return data?.data;
}
