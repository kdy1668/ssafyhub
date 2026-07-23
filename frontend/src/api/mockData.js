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
    serverStatus: "RUNNING",
    deployStatus: "DEPLOYED",
    url: "https://shopping-api.example-deploy.com",
    createdAt: "2026-07-10T09:00:00Z",
  },
  {
    id: "dep-2",
    projectName: "internal-dashboard",
    serverStatus: "RUNNING",
    deployStatus: "DEPLOYING",
    url: "https://internal-dashboard.example-deploy.com",
    createdAt: "2026-07-15T02:30:00Z",
  },
  {
    id: "dep-3",
    projectName: "batch-worker",
    serverStatus: "STOPPED",
    deployStatus: "FAILED",
    url: "https://batch-worker.example-deploy.com",
    createdAt: "2026-07-12T13:20:00Z",
  },
];

export function mockResourceUsageFor(id) {
  return {
    id,
    cpuPercent: Math.round(20 + Math.random() * 60),
    memoryUsedMb: Math.round(300 + Math.random() * 1200),
    memoryTotalMb: 2048,
    sampledAt: new Date().toISOString(),
  };
}

export const mockPosts = [
  {
    id: 3,
    title: "자유게시판 오픈했습니다",
    content: "자유롭게 의견을 나눠주세요. 잘 부탁드립니다!",
    createdAt: "2026-07-18T10:00:00Z",
  },
  {
    id: 2,
    title: "배포 후 502 에러 문의드립니다",
    content: "Spring Boot 프로젝트를 배포했는데 간헐적으로 502가 발생합니다. 혹시 같은 경험 있으신 분?",
    createdAt: "2026-07-16T05:30:00Z",
  },
  {
    id: 1,
    title: "환영합니다",
    content: "DeployHub 커뮤니티에 오신 것을 환영합니다.",
    createdAt: "2026-07-14T01:15:00Z",
  },
];

let nextMockPostId = 4;

export function nextPostId() {
  return nextMockPostId++;
}
