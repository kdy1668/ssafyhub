import { ref, reactive, computed } from "vue";
import { useQuery, useMutation, useQueryClient } from "@tanstack/vue-query";
import {
  fetchDeployments,
  getPresignedUrls,
  createDeployment as apiCreateDeployment,
  updateDeployment as apiUpdateDeployment,
  deleteDeployment as apiDeleteDeployment,
  putFileToS3WithProgress,
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

// 2. S3 직접 업로드 + 진행률/재시도 지원 배포 실행 엔진
export function useCreateDeployment() {
  // phase: idle | preparing | uploading | creating | done | error
  const phase = ref("idle");
  const deployError = ref(null);

  // 파일별 업로드 상태 (진행률 UI / 재시도 UI 의 데이터 소스)
  const uploads = reactive({
    backend: { label: "백엔드 JAR", name: "", progress: 0, status: "idle", error: null },
    frontend: { label: "프론트엔드 ZIP", name: "", progress: 0, status: "idle", error: null },
  });

  // 재시도를 위해 마지막 요청 인자를 보관
  let lastArgs = null;

  const isDeploying = computed(() =>
    ["preparing", "uploading", "creating"].includes(phase.value)
  );
  const hasError = computed(() => phase.value === "error");
  // 업로드 단계에서 실패한 파일 목록
  const failedUploads = computed(() =>
    Object.entries(uploads)
      .filter(([, u]) => u.status === "error")
      .map(([key]) => key)
  );

  function resetUpload(entry, name) {
    entry.name = name;
    entry.progress = 0;
    entry.status = "idle";
    entry.error = null;
  }

  function reset() {
    phase.value = "idle";
    deployError.value = null;
    resetUpload(uploads.backend, "");
    resetUpload(uploads.frontend, "");
    lastArgs = null;
  }

  async function runDeploy(args) {
    const {
      backendFile, frontendFile,
      projectName, runtimeVersion, internalPort,
      databaseName, databasePassword, envList,
      description = "", visibility = "PRIVATE",
      simulateFailure = false,
    } = args;

    deployError.value = null;
    resetUpload(uploads.backend, backendFile.name);
    resetUpload(uploads.frontend, frontendFile.name);

    try {
      // [STEP 1] Presigned URL 발급
      phase.value = "preparing";
      const presignedRes = await getPresignedUrls(backendFile.name, frontendFile.name);
      const { backendArtifact, frontendArtifact } = presignedRes.data;

      // [STEP 2] S3 업로드 (진행률 추적, 두 파일 병렬)
      phase.value = "uploading";

      const uploadOne = async (entry, url, file, failThisOne) => {
        entry.status = "uploading";
        entry.error = null;
        try {
          await putFileToS3WithProgress(
            url,
            file,
            (p) => { entry.progress = p; },
            { simulateFailure: failThisOne }
          );
          entry.status = "success";
        } catch (err) {
          entry.status = "error";
          entry.error = err.message || "업로드 실패";
          throw err;
        }
      };

      // allSettled 로 두 파일을 모두 시도 → 부분 실패도 각 상태에 반영
      const results = await Promise.allSettled([
        uploadOne(uploads.backend, backendArtifact.uploadUrl, backendFile, false),
        // 테스트용 실패 시뮬레이션은 프론트엔드 파일에만 적용
        uploadOne(uploads.frontend, frontendArtifact.uploadUrl, frontendFile, simulateFailure),
      ]);

      const rejected = results.find((r) => r.status === "rejected");
      if (rejected) {
        throw rejected.reason instanceof Error
          ? rejected.reason
          : new Error("파일 업로드에 실패했습니다.");
      }

      // [STEP 3] 최종 배포 요청 생성
      phase.value = "creating";
      await apiCreateDeployment({
        projectName,
        backendArtifactId: backendArtifact.artifactId,
        frontendArtifactId: frontendArtifact.artifactId,
        runtimeVersion,
        internalPort: Number(internalPort),
        databaseName,
        databasePassword,
        description,
        visibility,
        env: envList,
      });

      phase.value = "done";
      return true;
    } catch (error) {
      console.error("배포 프로세스 에러:", error);
      deployError.value = error.message || "배포 요청 중 알 수 없는 오류가 발생했습니다.";
      phase.value = "error";
      throw error;
    }
  }

  // 최초 실행: 인자를 보관하고 실행
  const executeDeploy = (args) => {
    lastArgs = args;
    return runDeploy(args);
  };

  // 재시도: 보관된 인자로 다시 실행 (없으면 무시)
  const retry = () => {
    if (!lastArgs) return Promise.resolve(false);
    return runDeploy(lastArgs);
  };

  return {
    phase,
    uploads,
    isDeploying,
    hasError,
    failedUploads,
    deployError,
    executeDeploy,
    retry,
    reset,
  };
}

// 3. 배포 설명/공개여부 수정 (마이페이지 편집용)
export function useUpdateDeployment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => apiUpdateDeployment(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deployments"] });
    },
  });
}

// 4. 배포 삭제 (운영 제어)
export function useDeleteDeployment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => apiDeleteDeployment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["deployments"] });
    },
  });
}