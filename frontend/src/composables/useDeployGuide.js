import { useMutation } from "@tanstack/vue-query";
import { generateDeployGuide } from "../api/deployGuide";

// 배포 가이드 생성 뮤테이션. 프로젝트 분석은 서버(AI)가 하고, 여기선 호출만.
export function useDeployGuide() {
  return useMutation({ mutationFn: generateDeployGuide });
}
