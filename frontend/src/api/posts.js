import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockPosts, nextPostId, mockUser } from "./mockData";

// mock: 현재 사용자가 좋아요한 게시글 id 집합 (세션 한정)
const likedPostIds = new Set();

export async function fetchPosts() {
  if (USE_MOCKS) {
    await mockDelay(400);
    return [...mockPosts]
      .sort((a, b) => b.id - a.id)
      .map((p) => ({ ...p, liked: likedPostIds.has(p.id) }));
  }
  const { data } = await apiClient.get("/api/posts");
  return data;
}

export async function fetchPost(id) {
  if (USE_MOCKS) {
    await mockDelay(300);
    const post = mockPosts.find((p) => String(p.id) === String(id));
    if (!post) {
      throw new Error("게시글을 찾을 수 없습니다.");
    }
    return { ...post, liked: likedPostIds.has(post.id) };
  }
  const { data } = await apiClient.get(`/api/posts/${id}`);
  return data;
}

export async function toggleLike(id) {
  if (USE_MOCKS) {
    await mockDelay(200);
    const post = mockPosts.find((p) => String(p.id) === String(id));
    if (!post) {
      throw new Error("게시글을 찾을 수 없습니다.");
    }
    const liked = likedPostIds.has(post.id);
    if (liked) {
      likedPostIds.delete(post.id);
      post.likes = Math.max(0, (post.likes ?? 1) - 1);
    } else {
      likedPostIds.add(post.id);
      post.likes = (post.likes ?? 0) + 1;
    }
    return { liked: !liked, likes: post.likes };
  }
  const { data } = await apiClient.post(`/api/posts/${id}/like`);
  return data;
}

export async function createPost(payload) {
  if (USE_MOCKS) {
    await mockDelay(500);
    if (!payload.title) {
      throw new Error("제목은 필수입니다.");
    }
    const type = payload.type === "deploy" ? "deploy" : "free";
    const post = {
      id: nextPostId(),
      type,
      title: payload.title,
      content: payload.content ?? "",
      author: mockUser.name,
      team: mockUser.team,
      likes: 0,
      commentCount: 0,
      createdAt: new Date().toISOString(),
    };
    // 배포 공유 타입이면 배포 정보 첨부
    if (type === "deploy" && payload.deployment?.projectName) {
      post.deployment = {
        projectName: payload.deployment.projectName,
        url: payload.deployment.url ?? "",
        deployStatus: payload.deployment.deployStatus ?? "DEPLOYED",
      };
    }
    mockPosts.push(post);
    return post;
  }
  const { data } = await apiClient.post("/api/posts", payload);
  return data;
}

export async function updatePost(id, payload) {
  if (USE_MOCKS) {
    await mockDelay(500);
    const post = mockPosts.find((p) => String(p.id) === String(id));
    if (!post) {
      throw new Error("게시글을 찾을 수 없습니다.");
    }
    post.title = payload.title;
    post.content = payload.content ?? "";
    if (payload.type) post.type = payload.type === "deploy" ? "deploy" : "free";

    // 배포 공유 → 배포 정보 갱신, 자유글 → 배포 정보 제거
    if (post.type === "deploy" && payload.deployment?.projectName) {
      post.deployment = {
        projectName: payload.deployment.projectName,
        url: payload.deployment.url ?? "",
        deployStatus: payload.deployment.deployStatus ?? post.deployment?.deployStatus ?? "DEPLOYED",
      };
    } else if (post.type === "free") {
      delete post.deployment;
    }
    return post;
  }
  const { data } = await apiClient.put(`/api/posts/${id}`, payload);
  return data;
}

export async function deletePost(id) {
  if (USE_MOCKS) {
    await mockDelay(400);
    const index = mockPosts.findIndex((p) => String(p.id) === String(id));
    if (index !== -1) {
      mockPosts.splice(index, 1);
    }
    return;
  }
  await apiClient.delete(`/api/posts/${id}`);
}
