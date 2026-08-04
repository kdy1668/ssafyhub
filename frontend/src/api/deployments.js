import { apiClient } from "./client";

// 관리자용 전체 배포 목록 조회 (AdminPage 전용). "내 배포 상태 1건" 조회(GET /api/v1/deployments)와
// 경로가 겹치지 않도록 관리자용은 /api/v1/admin/deployments를 쓰기로 함
// ⚠️ 백엔드 미구현 — 현재 호출하면 404. 경로만 예약된 상태다.
export async function fetchDeployments() {
  const { data } = await apiClient.get("/api/v1/admin/deployments");
  return data;
}

// 로그인한 사용자의 배포 상태 1건 조회 (사용자당 배포는 1개, 백엔드 GET /api/v1/deployments)
export async function fetchMyDeployment() {
  const { data } = await apiClient.get("/api/v1/deployments");
  return data?.data;
}

// 배포 삭제 (백엔드 DELETE /api/v1/deployments/{id}).
// 백엔드가 deploy-engine에 k8s 리소스(Deployment/Service/Ingress/Middleware) 정리를 먼저 요청하고,
// 성공하면 배포 행과 환경변수·로그 이력까지 지운다. 되돌릴 수 없다.
export async function deleteDeployment(deploymentId) {
  const { data } = await apiClient.delete(`/api/v1/deployments/${deploymentId}`);
  return data?.data;
}

export async function fetchDeploymentResources(deploymentId) {
  // 백엔드: ApiResponse { data: { id, cpuPercent, memoryUsedMb, memoryTotalMb,
  //   networkReceiveBytes, networkTransmitBytes, requestsTotal, sampledAt } }
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/resources`);
  return data?.data;
}

// 배포 리소스 사용량 시계열(그래프용). 지표별로 [{time,value}] 배열
// (cpu %, memory MB, networkReceive/networkTransmit MB, requests 건수).
export async function fetchDeploymentResourceHistory(deploymentId, minutes = 30) {
  const { data } = await apiClient.get(
    `/api/v1/deployments/${deploymentId}/resources/history?minutes=${minutes}`
  );
  return (
    data?.data ?? {
      cpu: [],
      memory: [],
      networkReceive: [],
      networkTransmit: [],
      requests: [],
    }
  );
}

// 배포된 앱의 백엔드 런타임 로그(stdout/stderr) 최근 tail줄 스냅샷(AWS 콘솔 로그 보기 방식).
export async function fetchRuntimeLogs(deploymentId, tail = 300) {
  const { data } = await apiClient.get(
    `/api/v1/deployments/${deploymentId}/runtime-logs?tail=${tail}`
  );
  return data?.data ?? "";
}

// 현재 배포에 설정된 환경변수 목록 조회(복호화된 값, 수정 화면 초기값 채우기용).
export async function fetchDeploymentEnv(deploymentId) {
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/env`);
  return data?.data ?? [];
}

// 환경변수만 새 값으로 교체하고 같은 아티팩트로 재배포 요청(아티팩트 재업로드 불필요).
// env: [{ key, value }, ...]
export async function updateDeploymentEnv(deploymentId, env) {
  const { data } = await apiClient.patch(`/api/v1/deployments/${deploymentId}/env`, env);
  return data?.data;
}

// 배포 진행 로그(깃헙 액션식 단계별). LogViewer가 쓰는 { time, status, message } 형태로 정규화.
export async function fetchDeploymentLogs(deploymentId) {
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

// presignedRes.data 구조에 맞게 { data: { ... } } 형태를 그대로 반환한다.
// schemaFileName은 선택 - MyBatis 등 자동 테이블 생성이 안 되는 프레임워크를 쓸 때만 넘긴다.
export async function getPresignedUrls(backendFileName, frontendFileName, schemaFileName) {
  const { data } = await apiClient.post("/api/v1/artifacts/presigned-urls", {
    backendFileName,
    frontendFileName,
    schemaFileName: schemaFileName || undefined,
  });
  return data;
}

/**
 * S3(presigned URL)로 파일을 PUT 업로드하면서 진행률을 콜백으로 전달한다.
 * XMLHttpRequest 의 upload.onprogress 로 실제 진행률을 추적한다.
 *
 * @param {string} url            presigned upload URL
 * @param {File}   file           업로드할 파일
 * @param {(p:number)=>void} onProgress  0~100 진행률 콜백
 * @param {{ contentType?: string }} [opts]
 * @returns {Promise<void>}
 */
export function putFileToS3WithProgress(url, file, onProgress, opts = {}) {
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
  // ApiResponse { success, code, message, data } → data(DeploymentCreateResponse) 반환
  const { data } = await apiClient.post("/api/v1/deployments", deployData);
  return data?.data;
}
