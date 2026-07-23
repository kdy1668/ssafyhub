import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("auth_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message || error.message || "요청 처리 중 오류가 발생했습니다.";
    return Promise.reject(new Error(message));
  }
);

export const USE_MOCKS = import.meta.env.VITE_USE_MOCKS !== "false";

export function mockDelay(ms = 500) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
