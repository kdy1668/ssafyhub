import { useMutation } from "@tanstack/vue-query";
import { getPresignedUploadUrl, uploadFileToS3 } from "../api/upload";

export function usePresignedUpload() {
  return useMutation({
    mutationFn: async (file) => {
      const { uploadUrl, key } = await getPresignedUploadUrl(file.name, file.type);
      await uploadFileToS3(uploadUrl, file);
      return { key, fileName: file.name };
    },
  });
}
