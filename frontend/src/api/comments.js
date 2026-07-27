import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockComments, mockPosts, nextCommentId, mockUser } from "./mockData";

export async function fetchComments(postId) {
  if (USE_MOCKS) {
    await mockDelay(300);
    return mockComments
      .filter((c) => String(c.postId) === String(postId))
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  }
  const { data } = await apiClient.get(`/api/posts/${postId}/comments`);
  return data;
}

export async function createComment(postId, payload) {
  if (USE_MOCKS) {
    await mockDelay(400);
    if (!payload.content?.trim()) {
      throw new Error("댓글 내용을 입력해주세요.");
    }
    const comment = {
      id: nextCommentId(),
      postId: Number.isNaN(Number(postId)) ? postId : Number(postId),
      author: mockUser.name,
      team: mockUser.team,
      content: payload.content.trim(),
      createdAt: new Date().toISOString(),
    };
    mockComments.push(comment);
    // 게시글 댓글 수 동기화
    const post = mockPosts.find((p) => String(p.id) === String(postId));
    if (post) post.commentCount = (post.commentCount ?? 0) + 1;
    return comment;
  }
  const { data } = await apiClient.post(`/api/posts/${postId}/comments`, payload);
  return data;
}

export async function deleteComment(postId, commentId) {
  if (USE_MOCKS) {
    await mockDelay(300);
    const index = mockComments.findIndex((c) => String(c.id) === String(commentId));
    if (index !== -1) {
      mockComments.splice(index, 1);
      const post = mockPosts.find((p) => String(p.id) === String(postId));
      if (post) post.commentCount = Math.max(0, (post.commentCount ?? 1) - 1);
    }
    return;
  }
  await apiClient.delete(`/api/posts/${postId}/comments/${commentId}`);
}
