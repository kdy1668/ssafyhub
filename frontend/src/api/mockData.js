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
