import { defineStore } from "pinia";
import { loginRequest, signupRequest } from "../api/auth";

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
    logout() {
      this.member = null;
      localStorage.removeItem("auth_member");
      // 과거 토큰 기반 키가 남아 있다면 정리한다.
      localStorage.removeItem("auth_token");
      localStorage.removeItem("auth_role");
    },
  },
});
