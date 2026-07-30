import { defineStore } from "pinia";
import { loginRequest, logoutRequest, signupRequest } from "../api/auth";

// 토큰은 HttpOnly 쿠키로 관리되므로 프론트에 저장하지 않는다.
// 로그인 응답의 member 정보만 전역 상태로 보관하고, 새로고침 대비 localStorage에 캐시한다.
function loadMember() {
  try {
    return JSON.parse(localStorage.getItem("auth_member")) || null;
  } catch {
    return null;
  }
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    member: loadMember(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.member),
    isAdmin: (state) => state.member?.role === "ADMIN",
  },
  actions: {
    async login({ studentId, password }) {
      const member = await loginRequest({ studentId, password });
      this.setMember(member);
    },
    async signup({ team, name, studentId, password, passwordConfirm }) {
      return signupRequest({ team, name, studentId, password, passwordConfirm });
    },
    setMember(member) {
      this.member = member;
      localStorage.setItem("auth_member", JSON.stringify(member));
    },
    // 사용자가 로그아웃을 누른 경우: 서버에 Refresh Token 폐기 + 쿠키 삭제를 요청한 뒤 로컬 상태를 정리한다.
    async logout() {
      try {
        await logoutRequest();
      } catch {
        // 서버 폐기에 실패해도(네트워크 오류 등) 클라이언트 상태는 반드시 정리한다.
      } finally {
        this.clearAuth();
      }
    },
    // 세션 만료(재발급 실패)처럼 서버 호출이 무의미한 경우를 위한 로컬 정리 전용 액션.
    // 이미 Refresh Token이 죽은 상태이므로 로그아웃 API를 호출하지 않는다.
    clearAuth() {
      this.member = null;
      localStorage.removeItem("auth_member");
      // 과거 토큰 기반 키가 남아 있다면 정리한다.
      localStorage.removeItem("auth_token");
      localStorage.removeItem("auth_role");
    },
  },
});
