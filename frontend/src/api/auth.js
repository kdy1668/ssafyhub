import { apiClient, USE_MOCKS, mockDelay } from "./client";
import { mockUser } from "./mockData";

export async function signupRequest({ team, name, studentId, password, passwordConfirm }) {
  if (USE_MOCKS) {
    await mockDelay(500);
    if (!name || !studentId || !password) {
      throw new Error("필수 항목을 모두 입력해주세요.");
    }
    if (password !== passwordConfirm) {
      throw new Error("비밀번호가 일치하지 않습니다.");
    }
    return { success: true, name, team };
  }
  const { data } = await apiClient.post("/api/v1/auth/signup", {
    teamName: team,
    name,
    studentNumber: studentId,
    password,
    passwordConfirm,
  });
  return data;
}

export async function loginRequest({ studentId, password }) {
  if (USE_MOCKS) {
    await mockDelay(500);
    if (!studentId || !password) {
      throw new Error("학번과 비밀번호를 입력해주세요.");
    }
    // 백엔드 없이 동작하도록 임의의 mock 토큰을 발급합니다.
    // 테스트 편의: 학번이 "admin"이면 관리자 권한으로 로그인됩니다.
    const role = studentId === "admin" ? "ADMIN" : "USER";
    return { token: `mock-token-${mockUser.id}`, user: { ...mockUser, role } };
  }
  const { data } = await apiClient.post("/api/users/login", { studentId, password });
  return data;
}
