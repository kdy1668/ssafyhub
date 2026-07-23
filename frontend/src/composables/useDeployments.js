import { ref } from "vue";
import { useQuery } from "@tanstack/vue-query";
import { 
  fetchDeployments, 
  getPresignedUrls, 
  createDeployment as apiCreateDeployment 
} from "../api/deployments";

// 🚀 스펙 반영: 3초마다 호출
const POLL_INTERVAL_MS = 3000; 

// 1. 목록 조회 및 상태 폴링 (기존 로직 업데이트)
export function useDeployments() {
  return useQuery({
    queryKey: ["deployments"],
    queryFn: fetchDeployments,
    refetchInterval: (query) => {
      const deployments = query.state.data;
      
      // 🚀 스펙 반영: status가 RUNNING 또는 FAILED가 되면 polling 멈춤
      const hasInProgress = deployments?.some((d) => 
        d.status !== "RUNNING" && d.status !== "FAILED"
      );
      
      return hasInProgress ? POLL_INTERVAL_MS : false;
    },
  });
}

// 2. 신규 추가: S3 직접 업로드 및 배포 실행 엔진
export function useCreateDeployment() {
  const isDeploying = ref(false);
  const deployError = ref(null);

  const executeDeploy = async ({
    backendFile, frontendFile,
    projectName, runtimeVersion, internalPort,
    databaseName, databasePassword, envList
  }) => {
    isDeploying.value = true;
    deployError.value = null;

    try {
      // [STEP 1] Presigned URL 발급 (apiClient 사용)
      const presignedRes = await getPresignedUrls(backendFile.name, frontendFile.name);
      
      // 백엔드 응답 구조(data.data)에 맞춰 꺼내기
      const { backendArtifact, frontendArtifact } = presignedRes.data;

      // [STEP 2 & 3] S3 직접 업로드 (Mock 환경 대응 추가)
      const uploadToS3 = async (url, file) => {
        // 🌟 Mock URL일 경우 실제 fetch를 타지 않고 가상 업로드 성공 처리
        if (url && url.includes("mock")) {
          await new Promise((resolve) => setTimeout(resolve, 500)); // 0.5초 대기 흉내
          return;
        }

        const response = await fetch(url, {
          method: "PUT",
          body: file
        });
        
        if (!response.ok) {
          throw new Error(`[${file.name}] S3 업로드 실패`);
        }
        return response;
      };

      // Promise.all로 두 파일 동시 업로드 진행
      await Promise.all([
        uploadToS3(backendArtifact.uploadUrl, backendFile),
        uploadToS3(frontendArtifact.uploadUrl, frontendFile)
      ]);

      // [STEP 4] 최종 배포 요청 생성
      await apiCreateDeployment({
        projectName,
        backendArtifactId: backendArtifact.artifactId,
        frontendArtifactId: frontendArtifact.artifactId,
        runtimeVersion,
        internalPort: Number(internalPort),
        databaseName,
        databasePassword,
        env: envList
      });

      return true;

    } catch (error) {
      console.error("배포 프로세스 에러:", error);
      deployError.value = error.message || "배포 요청 중 알 수 없는 오류가 발생했습니다.";
      throw error;
    } finally {
      isDeploying.value = false;
    }
  };

  return {
    isDeploying,
    deployError,
    executeDeploy
  };
}