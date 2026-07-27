import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockDeployments, mockResourceUsageFor } from "./mockData";

export async function fetchDeployments() {
  if (USE_MOCKS) {
    await mockDelay(500);
    return mockDeployments.map((item) => {
      if (item.deployStatus === "DEPLOYING" && Math.random() < 0.3) {
        return { ...item, deployStatus: "DEPLOYED" };
      }
      return item;
    });
  }
  const { data } = await apiClient.get("/api/v1/deployments");
  return data;
}

export async function fetchDeploymentResources(deploymentId) {
  if (USE_MOCKS) {
    await mockDelay(400);
    return mockResourceUsageFor(deploymentId);
  }
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/resources`);
  return data;
}

// 🚀 수정된 부분: presignedRes.data 구조에 맞게 { data: { ... } } 형태로 감싸줌
export async function getPresignedUrls(backendFileName, frontendFileName) {
  if (USE_MOCKS) {
    await mockDelay(500);
    return {
      data: {
        backendArtifact: {
          artifactId: 1,
          uploadUrl: "mock-s3-backend-upload-url",
        },
        frontendArtifact: {
          artifactId: 2,
          uploadUrl: "mock-s3-frontend-upload-url",
        },
      }
    };
  }

  const { data } = await apiClient.post("/api/v1/artifacts/presigned-urls", {
    backendFileName,
    frontendFileName
  });
  return data; 
}

/**
 * S3(presigned URL)로 파일을 PUT 업로드하면서 진행률을 콜백으로 전달한다.
 * - 실제 환경: XMLHttpRequest 의 upload.onprogress 로 실제 진행률 추적
 * - mock 환경: presigned URL 이 mock 이면 실제 네트워크 없이 진행률을 시뮬레이션
 *
 * @param {string} url            presigned upload URL
 * @param {File}   file           업로드할 파일
 * @param {(p:number)=>void} onProgress  0~100 진행률 콜백
 * @param {{ simulateFailure?: boolean }} [opts]  테스트용 실패 시뮬레이션
 * @returns {Promise<void>}
 */
export function putFileToS3WithProgress(url, file, onProgress, opts = {}) {
  const isMock = !url || url.includes("mock");

  // --- mock: 진행률 시뮬레이션 ---
  if (isMock) {
    return new Promise((resolve, reject) => {
      let progress = 0;
      const timer = setInterval(() => {
        progress += Math.random() * 18 + 7; // 한 틱에 7~25%
        // 테스트용 실패: 70% 부근에서 강제 실패
        if (opts.simulateFailure && progress >= 65) {
          clearInterval(timer);
          onProgress(Math.min(Math.round(progress), 70));
          reject(new Error("업로드 중 네트워크 오류가 발생했습니다. (시뮬레이션)"));
          return;
        }
        if (progress >= 100) {
          progress = 100;
          onProgress(100);
          clearInterval(timer);
          resolve();
          return;
        }
        onProgress(Math.round(progress));
      }, 200);
    });
  }

  // --- real: XHR 로 실제 진행률 추적 ---
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    if (file.type) xhr.setRequestHeader("Content-Type", file.type);

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress(100);
        resolve();
      } else {
        reject(new Error(`S3 업로드 실패 (HTTP ${xhr.status})`));
      }
    };
    xhr.onerror = () => reject(new Error("네트워크 오류로 업로드에 실패했습니다."));
    xhr.onabort = () => reject(new Error("업로드가 중단되었습니다."));
    xhr.send(file);
  });
}

export async function createDeployment(deployData) {
  if (USE_MOCKS) {
    await mockDelay(500);
    
    const newMockDeployment = {
      id: Date.now(),
      projectName: deployData.projectName || "demo-service",
      deployStatus: "DEPLOYING",
      runtimeVersion: deployData.runtimeVersion || "17",
      internalPort: deployData.internalPort || "8080",
      databaseName: deployData.databaseName || "app_db",
      description: deployData.description || "",
      visibility: deployData.visibility === "PUBLIC" ? "PUBLIC" : "PRIVATE",
    };

    mockDeployments.unshift(newMockDeployment);

    setTimeout(() => {
      newMockDeployment.deployStatus = "DEPLOYED";
    }, 5000);

    return { success: true, data: newMockDeployment };
  }

  const { data } = await apiClient.post("/api/v1/deployments", deployData);
  return data;
}

// 배포의 설명/공개여부 수정 (마이페이지에서 사용)
export async function updateDeployment(deploymentId, payload) {
  if (USE_MOCKS) {
    await mockDelay(400);
    const dep = mockDeployments.find((d) => String(d.id) === String(deploymentId));
    if (!dep) {
      throw new Error("배포를 찾을 수 없습니다.");
    }
    if (payload.description !== undefined) dep.description = payload.description;
    if (payload.visibility !== undefined) {
      dep.visibility = payload.visibility === "PUBLIC" ? "PUBLIC" : "PRIVATE";
    }
    return { ...dep };
  }
  const { data } = await apiClient.patch(`/api/v1/deployments/${deploymentId}`, payload);
  return data;
}