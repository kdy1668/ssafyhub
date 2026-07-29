import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockShowcaseDeployments } from "./mockData";

// 공개(PUBLIC) 배포 목록 조회 — 인증 불필요
export async function fetchPublicShowcases(page = 0, size = 20) {
  if (USE_MOCKS) {
    await mockDelay(400);
    const start = page * size;
    return {
      deployments: mockShowcaseDeployments.slice(start, start + size),
      page,
      size,
      totalElements: mockShowcaseDeployments.length,
    };
  }
  const { data } = await apiClient.get("/api/v1/showcase/public", { params: { page, size } });
  return data?.data;
}

// 공개 배포 상세 조회 — 인증 불필요
export async function fetchShowcase(id) {
  if (USE_MOCKS) {
    await mockDelay(300);
    const found = mockShowcaseDeployments.find((d) => String(d.deploymentId) === String(id));
    if (!found) throw new Error("존재하지 않는 서비스입니다.");
    return found;
  }
  const { data } = await apiClient.get(`/api/v1/showcase/${id}`);
  return data?.data;
}
