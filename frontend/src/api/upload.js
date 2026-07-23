import axios from "axios";
import { apiClient, USE_MOCKS, mockDelay } from "./client";

export async function getPresignedUploadUrl(fileName, fileType) {
  if (USE_MOCKS) {
    await mockDelay(300);
    const key = `uploads/${Date.now()}-${fileName}`;
    return {
      uploadUrl: `https://mock-s3-bucket.example.com/${key}`,
      key,
    };
  }
  const { data } = await apiClient.get("/api/upload/presigned-url", {
    params: { fileName, fileType },
  });
  return data;
}

export async function uploadFileToS3(uploadUrl, file) {
  if (USE_MOCKS) {
    await mockDelay(800);
    return;
  }
  await axios.put(uploadUrl, file, {
    headers: { "Content-Type": file.type },
  });
}
