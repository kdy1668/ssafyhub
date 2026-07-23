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

    return { success: true, data: newMockDeployment };
  }

  const { data } = await apiClient.post("/api/v1/deployments", deployData);
  return data;
}