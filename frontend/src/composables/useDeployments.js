import { ref, reactive, computed } from "vue";
import { useQuery } from "@tanstack/vue-query";
import {
  fetchDeployments,
  fetchMyDeployment,
  getPresignedUrls,
  createDeployment as apiCreateDeployment,
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

// 1-1. 내 배포 상태 1건 조회 및 폴링 (사용자당 배포는 1개)
export function useMyDeployment() {
  return useQuery({
    queryKey: ["deployments", "me"],
    queryFn: fetchMyDeployment,
    retry: false,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      const inProgress = status && status !== "RUNNING" && status !== "FAILED";
      return inProgress ? POLL_INTERVAL_MS : false;
    },
  });
}

// 2. S3 직접 업로드 + 진행률/재시도 지원 배포 실행 엔진
export function useCreateDeployment() {
  // phase: idle | preparing | uploading | creating | done | error
  const phase = ref("idle");
  const deployError = ref(null);

  // 파일별 업로드 상태 (진행률 UI / 재시도 UI 의 데이터 소스). schema는 SQL 파일을 올릴 때만 채워진다.
  const uploads = reactive({
    backend: { label: "백엔드 아티팩트", name: "", progress: 0, status: "idle", error: null },
    frontend: { label: "프론트엔드 아티팩트", name: "", progress: 0, status: "idle", error: null },
    schema: { label: "DB 초기화 SQL", name: "", progress: 0, status: "idle", error: null },
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
    resetUpload(uploads.schema, "");
    lastArgs = null;
  }

  async function runDeploy(args) {
    const {
      backendFile, frontendFile, schemaFile,
      projectName, runtimeVersion, internalPort,
      databaseName, databasePassword, databaseEngine, neededServices, envList,
      visibility, description,
    } = args;

    deployError.value = null;
    resetUpload(uploads.backend, backendFile.name);
    resetUpload(uploads.frontend, frontendFile.name);
    resetUpload(uploads.schema, schemaFile ? schemaFile.name : "");

    try {
      // [STEP 1] Presigned URL 발급 (schemaFile은 선택 - DB 초기화 SQL을 올릴 때만)
      phase.value = "preparing";
      const presignedRes = await getPresignedUrls(
        backendFile.name, frontendFile.name, schemaFile ? schemaFile.name : undefined
      );
      const { backendArtifact, frontendArtifact, schemaArtifact } = presignedRes.data;

      // [STEP 2] S3 업로드 (진행률 추적, 병렬)
      phase.value = "uploading";

      const uploadOne = async (entry, url, file, contentType) => {
        entry.status = "uploading";
        entry.error = null;
        try {
          await putFileToS3WithProgress(
            url,
            file,
            (p) => { entry.progress = p; },
            { contentType }
          );
          entry.status = "success";
        } catch (err) {
          entry.status = "error";
          entry.error = err.message || "업로드 실패";
          throw err;
        }
      };

      // allSettled 로 모든 파일을 시도 → 부분 실패도 각 상태에 반영
      // 백엔드는 .jar 또는 .zip(Dockerfile 포함), 프론트엔드는 항상 .zip(Dockerfile 포함).
      const backendContentType = backendFile.name.toLowerCase().endsWith(".zip")
        ? "application/zip"
        : "application/java-archive";
      const uploadTasks = [
        uploadOne(uploads.backend, backendArtifact.uploadUrl, backendFile, backendContentType),
        uploadOne(uploads.frontend, frontendArtifact.uploadUrl, frontendFile, "application/zip"),
      ];
      if (schemaFile && schemaArtifact) {
        uploadTasks.push(uploadOne(uploads.schema, schemaArtifact.uploadUrl, schemaFile, "application/sql"));
      }
      const results = await Promise.allSettled(uploadTasks);

      const rejected = results.find((r) => r.status === "rejected");
      if (rejected) {
        throw rejected.reason instanceof Error
          ? rejected.reason
          : new Error("파일 업로드에 실패했습니다.");
      }

      // [STEP 3] 최종 배포 요청 생성 (아티팩트 업로드가 모두 성공한 뒤에만 호출)
      phase.value = "creating";
      const deployment = await apiCreateDeployment({
        projectName,
        backendArtifactId: backendArtifact.artifactId,
        frontendArtifactId: frontendArtifact.artifactId,
        runtimeVersion,
        internalPort: Number(internalPort),
        databaseName,
        databasePassword,
        databaseEngine,
        schemaArtifactId: schemaFile ? schemaArtifact?.artifactId : undefined,
        neededServices,
        env: envList,
        visibility,
        description,
      });

      phase.value = "done";
      return deployment;
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