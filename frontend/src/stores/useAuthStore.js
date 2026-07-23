import { defineStore } from "pinia";
import { loginRequest, signupRequest } from "../api/auth";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    isAuthenticated: Boolean(localStorage.getItem("auth_token")),
  }),
  actions: {
    async login({ studentId, password }) {
      const data = await loginRequest({ studentId, password });
      localStorage.setItem("auth_token", data?.token ?? "");
      this.isAuthenticated = true;
    },
    async signup({ team, name, studentId, password, passwordConfirm }) {
      return signupRequest({ team, name, studentId, password, passwordConfirm });
    },
    logout() {
      localStorage.removeItem("auth_token");
      this.isAuthenticated = false;
    },
  },
});