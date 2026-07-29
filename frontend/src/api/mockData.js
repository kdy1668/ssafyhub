export const mockUser = {
  id: "u-1001",
  name: "김서비",
  team: "플랫폼팀",
  email: "seobi.kim@example.com",
};

export const mockDeployments = [
  {
    id: "dep-1",
    projectName: "shopping-api",
    owner: "김서비",
    serverStatus: "RUNNING",
    deployStatus: "DEPLOYED",
    url: "https://shopping-api.example-deploy.com",
    createdAt: "2026-07-10T09:00:00Z",
  },
  {
    id: "dep-2",
    projectName: "internal-dashboard",
    owner: "이도윤",
    serverStatus: "RUNNING",
    deployStatus: "DEPLOYING",
    url: "https://internal-dashboard.example-deploy.com",
    createdAt: "2026-07-15T02:30:00Z",
  },
  {
    id: "dep-3",
    projectName: "batch-worker",
    owner: "김서비",
    serverStatus: "STOPPED",
    deployStatus: "FAILED",
    url: "https://batch-worker.example-deploy.com",
    createdAt: "2026-07-12T13:20:00Z",
  },
];

// 로그인한 사용자의 배포 상태 1건 (GET /api/v1/deployments 목업)
export const mockMyDeployment = {
  deploymentId: "dep-1",
  projectName: "shopping-api",
  description: "쇼핑몰 백엔드 API 서비스입니다. 상품/주문 엔드포인트를 제공합니다.",
  visibility: "PUBLIC",
  status: "DEPLOYING",
  displayStatus: "배포 환경에 적용 중",
  endpointUrl: "https://shopping-api.example-deploy.com",
  statusMessage: null,
  updatedAt: "2026-07-20T08:00:00Z",
};

export function mockResourceUsageFor(id) {
  return {
    id,
    cpuPercent: Math.round(20 + Math.random() * 60),
    memoryUsedMb: Math.round(300 + Math.random() * 1200),
    memoryTotalMb: 2048,
    sampledAt: new Date().toISOString(),
  };
}

// 쇼케이스: PUBLIC으로 공개 설정된 배포만 노출 (PublicDeploymentResponse 형태)
export const mockShowcaseDeployments = [
  {
    deploymentId: "dep-1",
    teamName: "플랫폼팀",
    projectName: "shopping-api",
    description: "쇼핑몰 백엔드 API 서비스입니다. 상품/주문 엔드포인트를 제공합니다.",
    endpointUrl: "https://shopping-api.example-deploy.com",
    updatedAt: "2026-07-20T08:00:00Z",
  },
  {
    deploymentId: "dep-2",
    teamName: "프론트팀",
    projectName: "internal-dashboard",
    description: "사내 운영 대시보드 서비스입니다.",
    endpointUrl: "https://internal-dashboard.example-deploy.com",
    updatedAt: "2026-07-19T02:30:00Z",
  },
];
