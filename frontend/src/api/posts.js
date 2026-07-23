import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockPosts, nextPostId } from "./mockData";

export async function fetchPosts() {
  if (USE_MOCKS) {
    await mockDelay(400);
    return [...mockPosts].sort((a, b) => b.id - a.id);
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
    return post;
  }
  const { data } = await apiClient.get(`/api/posts/${id}`);
  return data;
}

export async function createPost(payload) {
  if (USE_MOCKS) {
    await mockDelay(500);
    if (!payload.title) {
      throw new Error("제목은 필수입니다.");
    }
    const post = {
      id: nextPostId(),
      title: payload.title,
      content: payload.content ?? "",
      createdAt: new Date().toISOString(),
    };
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
