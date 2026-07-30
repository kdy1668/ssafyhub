import { apiClient } from "./client";

// 공개(PUBLIC) 배포 목록 조회 — 인증 불필요
export async function fetchPublicShowcases(page = 0, size = 20) {
  const { data } = await apiClient.get("/api/v1/showcase/public", { params: { page, size } });
  return data?.data;
}

// 공개 배포 상세 조회 — 인증 불필요
export async function fetchShowcase(id) {
  const { data } = await apiClient.get(`/api/v1/showcase/${id}`);
  return data?.data;
}
