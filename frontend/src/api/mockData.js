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
    description: "쇼핑몰 상품/주문 백엔드 API 서버",
    visibility: "PUBLIC",
    createdAt: "2026-07-10T09:00:00Z",
  },
  {
    id: "dep-2",
    projectName: "internal-dashboard",
    owner: "이도윤",
    serverStatus: "RUNNING",
    deployStatus: "DEPLOYING",
    url: "https://internal-dashboard.example-deploy.com",
    description: "사내 운영 대시보드",
    visibility: "PRIVATE",
    createdAt: "2026-07-15T02:30:00Z",
  },
  {
    id: "dep-3",
    projectName: "batch-worker",
    owner: "김서비",
    serverStatus: "STOPPED",
    deployStatus: "FAILED",
    url: "https://batch-worker.example-deploy.com",
    description: "정산 배치 워커",
    visibility: "PRIVATE",
    createdAt: "2026-07-12T13:20:00Z",
  },
];

// 배포별 목업 로그 (요청 시마다 최근 런타임 로그 몇 줄이 랜덤 추가됨)
export function mockLogsFor(id) {
  const now = Date.now();
  const t = (offsetSec) => new Date(now - offsetSec * 1000).toTimeString().slice(0, 8);

  const base = [
    { off: 62, level: "INFO", message: "Deployment pipeline started (artifact: build.jar)" },
    { off: 60, level: "INFO", message: "Pulling base image eclipse-temurin:21-jre..." },
    { off: 55, level: "INFO", message: "Building container image... done (12.4s)" },
    { off: 50, level: "INFO", message: "Pushing image to registry: shopping-api:latest" },
    { off: 44, level: "INFO", message: "Applying k3s manifests (Deployment, Service, Ingress)" },
    { off: 40, level: "DEBUG", message: "Waiting for pod readiness probe on :8080/actuator/health" },
    { off: 33, level: "WARN", message: "Readiness probe failed once (connection refused), retrying..." },
    { off: 30, level: "INFO", message: "Pod is Ready. Rolling update complete (1/1 available)" },
    { off: 28, level: "INFO", message: "Ingress route registered: https://shopping-api.example-deploy.com" },
    { off: 25, level: "INFO", message: "Spring Boot started in 6.821s (JVM running for 7.4s)" },
    { off: 18, level: "INFO", message: 'GET /api/products 200 (14ms)' },
    { off: 12, level: "INFO", message: 'GET /api/products/42 200 (8ms)' },
    { off: 7, level: "WARN", message: "Slow query detected: SELECT * FROM orders (312ms)" },
    { off: 3, level: "INFO", message: 'POST /api/orders 201 (39ms)' },
  ];

  // 요청 시점마다 최근 요청 로그 1~2줄을 랜덤으로 덧붙여 실시간 느낌
  const samples = [
    { level: "INFO", message: 'GET /api/products 200 (11ms)' },
    { level: "INFO", message: 'GET /api/health 200 (2ms)' },
    { level: "DEBUG", message: "HikariCP - pool stats (active=2, idle=8)" },
    { level: "WARN", message: "Rate limit near threshold for client 10.0.0.7" },
    { level: "ERROR", message: "Upstream timeout calling payment-service (2000ms)" },
  ];
  const extraCount = 1 + Math.floor(Math.random() * 2);
  const extra = [];
  for (let i = 0; i < extraCount; i++) {
    const s = samples[Math.floor(Math.random() * samples.length)];
    extra.push({ time: t(0), level: s.level, message: s.message });
  }

  return [
    ...base.map((l) => ({ time: t(l.off), level: l.level, message: l.message })),
    ...extra,
  ];
}

export function mockResourceUsageFor(id) {
  return {
    id,
    cpuPercent: Math.round(20 + Math.random() * 60),
    memoryUsedMb: Math.round(300 + Math.random() * 1200),
    memoryTotalMb: 2048,
    sampledAt: new Date().toISOString(),
  };
}

// 메인 피드는 "배포 서비스 공유(type: deploy)"와 "자유 글(type: free)"이 섞인 목록입니다.
export const mockPosts = [
  {
    id: 5,
    type: "deploy",
    title: "shopping-api 새로 배포했어요 🚀 피드백 환영합니다",
    content:
      "쇼핑몰 백엔드 API를 새로 배포했습니다. 상품/주문 엔드포인트 위주로 테스트해봤는데, 부하 테스트 같이 해보실 분 계실까요?",
    author: "김서비",
    team: "플랫폼팀",
    deployment: {
      projectName: "shopping-api",
      url: "https://shopping-api.example-deploy.com",
      deployStatus: "DEPLOYED",
    },
    likes: 12,
    commentCount: 2,
    createdAt: "2026-07-20T08:00:00Z",
  },
  {
    id: 4,
    type: "deploy",
    title: "internal-dashboard 배포 진행 중입니다",
    content: "사내 운영 대시보드를 배포하고 있어요. 완료되면 접근 권한 공유드리겠습니다!",
    author: "이도윤",
    team: "프론트팀",
    deployment: {
      projectName: "internal-dashboard",
      url: "https://internal-dashboard.example-deploy.com",
      deployStatus: "DEPLOYING",
    },
    likes: 5,
    commentCount: 0,
    createdAt: "2026-07-19T02:30:00Z",
  },
  {
    id: 3,
    type: "free",
    title: "자유게시판 오픈했습니다",
    content: "자유롭게 의견을 나눠주세요. 잘 부탁드립니다!",
    author: "김서비",
    team: "플랫폼팀",
    likes: 8,
    commentCount: 1,
    createdAt: "2026-07-18T10:00:00Z",
  },
  {
    id: 2,
    type: "free",
    title: "배포 후 502 에러 문의드립니다",
    content: "Spring Boot 프로젝트를 배포했는데 간헐적으로 502가 발생합니다. 혹시 같은 경험 있으신 분?",
    author: "박하늘",
    team: "백엔드팀",
    likes: 3,
    commentCount: 2,
    createdAt: "2026-07-16T05:30:00Z",
  },
  {
    id: 1,
    type: "free",
    title: "환영합니다",
    content: "SSAFYHUB 커뮤니티에 오신 것을 환영합니다.",
    author: "관리자",
    team: "운영팀",
    likes: 15,
    commentCount: 0,
    createdAt: "2026-07-14T01:15:00Z",
  },
];

let nextMockPostId = 6;

export function nextPostId() {
  return nextMockPostId++;
}

// 게시글별 댓글 (postId 기준)
export const mockComments = [
  {
    id: 101,
    postId: 5,
    author: "이도윤",
    team: "프론트팀",
    content: "오 축하드려요! 주문 엔드포인트 부하 테스트 같이 해봐요 👍",
    createdAt: "2026-07-20T09:10:00Z",
  },
  {
    id: 102,
    postId: 5,
    author: "박하늘",
    team: "백엔드팀",
    content: "상품 목록 API 응답 속도가 인상적이네요. 캐시 적용하신 건가요?",
    createdAt: "2026-07-20T10:02:00Z",
  },
  {
    id: 103,
    postId: 3,
    author: "박하늘",
    team: "백엔드팀",
    content: "좋은 공간이네요. 자주 들르겠습니다!",
    createdAt: "2026-07-18T11:20:00Z",
  },
  {
    id: 104,
    postId: 2,
    author: "김서비",
    team: "플랫폼팀",
    content: "502면 대개 백엔드 컨테이너가 아직 안 떠서 그럴 수 있어요. readiness probe 확인해보세요.",
    createdAt: "2026-07-16T06:00:00Z",
  },
  {
    id: 105,
    postId: 2,
    author: "관리자",
    team: "운영팀",
    content: "인그레스 타임아웃 설정도 함께 점검 부탁드립니다.",
    createdAt: "2026-07-16T06:30:00Z",
  },
];

let nextMockCommentId = 106;

export function nextCommentId() {
  return nextMockCommentId++;
}
