import { apiClient } from "./client";

// AI 기능(배포 실패 진단 / 런타임 로그 이상 감지) API.
// 서버에서 ai.enabled가 꺼져 있으면 진단·감지 경로는 아예 매핑되지 않아 404가 온다.
// 그래서 화면은 먼저 fetchAiConfig()로 활성 여부를 확인하고 UI를 그린다.

/**
 * AI 기능 활성 여부와 클라이언트가 필요한 설정값.
 * 이 엔드포인트는 토글과 무관하게 항상 응답한다.
 * → { enabled, runtimeLogCooldownSeconds }
 */
export async function fetchAiConfig() {
  const { data } = await apiClient.get("/api/v1/ai/availability");
  return data?.data ?? { enabled: false, runtimeLogCooldownSeconds: 30 };
}

/**
 * 배포 실패 원인 진단. 서버가 (배포, 실패로그) 단위로 캐시하므로 여러 번 호출해도
 * AI 호출 비용은 늘지 않는다(응답의 cached=true면 이번엔 AI를 부르지 않았다는 뜻).
 */
export async function createFailureDiagnosis(deploymentId) {
  const { data } = await apiClient.post(`/api/v1/deployments/${deploymentId}/diagnosis`);
  return data?.data;
}

/** 저장된 최근 진단만 조회(AI 미호출). 없으면 204라서 data가 비어 있다. */
export async function fetchFailureDiagnosis(deploymentId) {
  const { data } = await apiClient.get(`/api/v1/deployments/${deploymentId}/diagnosis`);
  return data?.data ?? null;
}

/**
 * 런타임 로그 이상 감지. 사용자가 버튼을 눌렀을 때만 호출해야 한다.
 * 로그 폴링에 연결하면 화면을 열어두는 것만으로 비용이 계속 발생한다.
 */
export async function detectRuntimeLogAnomalies(deploymentId) {
  const { data } = await apiClient.post(
    `/api/v1/deployments/${deploymentId}/runtime-logs/detection`
  );
  return data?.data;
}
