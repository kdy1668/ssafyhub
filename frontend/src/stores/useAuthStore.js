import { defineStore } from "pinia";
import { loginRequest, signupRequest } from "../api/auth";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    isAuthenticated: Boolean(localStorage.getItem("auth_token")),
    // 관리자 여부: 로그인 응답의 role(ADMIN)로 결정. 새로고침 대비 localStorage 유지.
    isAdmin: localStorage.getItem("auth_role") === "ADMIN",
  }),
  actions: {
    async login({ studentId, password }) {
      const data = await loginRequest({ studentId, password });
      localStorage.setItem("auth_token", data?.token ?? "");
      this.isAuthenticated = true;

      // 백엔드가 내려주는 역할 기준. mock 환경에서는 user.role 사용.
      const role = data?.user?.role ?? data?.role ?? "USER";
      this.isAdmin = role === "ADMIN";
      localStorage.setItem("auth_role", role);
    },
    async signup({ team, name, studentId, password, passwordConfirm }) {
      return signupRequest({ team, name, studentId, password, passwordConfirm });
    },
    logout() {
      localStorage.removeItem("auth_token");
      localStorage.removeItem("auth_role");
      this.isAuthenticated = false;
      this.isAdmin = false;
    },
  },
});