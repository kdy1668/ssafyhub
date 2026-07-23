import { apiClient } from "./client";

export async function signupRequest({ team, name, studentId, password, passwordConfirm }) {
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
  const { data } = await apiClient.post("/api/users/login", { studentId, password });
  return data;
}