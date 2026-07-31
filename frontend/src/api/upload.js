import axios from "axios";
import { apiClient } from "./client";

// ⚠️ 백엔드 미구현 — GET /api/upload/presigned-url 은 존재하지 않는다(현재 호출하면 404).
// 실제 업로드 URL 발급은 api/deployments.js 의 getPresignedUrls(POST /api/v1/artifacts/presigned-urls)를 쓴다.
export async function getPresignedUploadUrl(fileName, fileType) {
  const { data } = await apiClient.get("/api/upload/presigned-url", {
    params: { fileName, fileType },
  });
  return data;
}

export async function uploadFileToS3(uploadUrl, file) {
  await axios.put(uploadUrl, file, {
    headers: { "Content-Type": file.type },
  });
}
