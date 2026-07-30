import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockDeployments, mockMyDeployment, mockResourceUsageFor } from "./mockData";

// 관리자용 전체 배포 목록 조회 (AdminPage 전용). "내 배포 상태 1건" 조회(GET /api/v1/deployments)와
// 경로가 겹치지 않도록 관리자용은 /api/v1/admin/deployments를 쓰기로 함 (백엔드 미구현, 경로만 예약)
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
  const { data } = await apiClient.get("/api/v1/admin/deployments");
  return data;
}

// 로그인한 사용자의 배포 상태 1건 조회 (사용자당 배포는 1개, 백엔드 GET /api/v1/deployments)
export async function fetchMyDeployment() {
  if (USE_MOCKS) {
    await mockDelay(400);
    if (mockMyDeployment.status === "DEPLOYING" && Math.random() < 0.3) {
      mockMyDeployment.status = "RUNNING";
      mockMyDeployment.displayStatus = "정상 기동";
      mockMyDeployment.updatedAt = new Date().toISOString();
    }
    return mockMyDeployment;
  }
  const { data } = await apiClient.get("/api/v1/deployments");
  return data?.data;
}

// 배포 삭제 (백엔드 DELETE /api/v1/deployments/{id}).
// 백엔드가 deploy-engine에 k8s 리소스(Deployment/Service/Ingress/Middleware) 정리를 먼저 요청하고,
// 성공하면 배포 행과 환경변수·로그 이력까지 지운다. 되돌릴 수 없다.
export async function deleteDeployment(deploymentId) {
  if (USE_MOCKS) {
    await mockDelay(500);
    const index = mockDeployments.findIndex((item) => item.id === deploymentId);
    if (index !== -1) mockDeployments.splice(index, 1);
    return { deploymentId };
  }
  const { data } = await apiClient.delete(`/api/v1/deployments/${deploymentId}`);
  return data?.data;
}

export async function fetchDeploymentResources(deploymentId) {
  if (USE_MOCKS) {
    await mockDelay(400);
    return mockResourceUsageFor(deploymentId);
  }
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/resources`);
  return data;
}

// 배포 진행 로그(깃헙 액션식 단계별). LogViewer가 쓰는 { time, status, message } 형태로 정규화.
export async function fetchDeploymentLogs(deploymentId) {
  if (USE_MOCKS) {
    await mockDelay(300);
    return [
      { time: "10:00:01", status: "PENDING", message: "배포 요청이 접수되었습니다." },
      { time: "10:00:03", status: "BUILDING", message: "상태 변경: BUILDING" },
      { time: "10:00:20", status: "BUILDING", message: "#5 transferring context: 52.38MB done" },
      { time: "10:00:31", status: "DEPLOYING", message: "상태 변경: DEPLOYING" },
      { time: "10:01:10", status: "RUNNING", message: "상태 변경: RUNNING" },
    ];
  }
  // 백엔드: ApiResponse.data = [{ logMessage, status, createdAt }, ...] (시간순)
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/logs`);
  const list = data?.data ?? [];
  return list.map((log) => ({
    time: log.createdAt
      ? new Date(log.createdAt).toLocaleTimeString("ko-KR", { hour12: false })
      : "",
    status: log.status,
    message: log.logMessage,
  }));
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
  // 주의: S3 presigned URL 직접 업로드이므로 공통 apiClient를 쓰지 않는다.
  //       (인증 쿠키/CSRF 헤더를 붙이지 않고, withCredentials=false 인 순수 XHR)
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    // 명세: JAR → application/java-archive, ZIP → application/zip
    const contentType = opts.contentType || file.type;
    if (contentType) xhr.setRequestHeader("Content-Type", contentType);

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
    };

    mockDeployments.unshift(newMockDeployment);

    setTimeout(() => {
      newMockDeployment.deployStatus = "DEPLOYED";
    }, 5000);

    // 실서버 응답(DeploymentCreateResponse) 형태로 반환
    return {
      deploymentId: newMockDeployment.id,
      status: "PENDING",
      displayStatus: "배포 요청 접수",
      expectedUrl: `https://${deployData.projectName || "demo"}.ssafyhub.site`,
      requestedAt: new Date().toISOString(),
    };
  }

  // ApiResponse { success, code, message, data } → data(DeploymentCreateResponse) 반환
  const { data } = await apiClient.post("/api/v1/deployments", deployData);
  return data?.data;
}