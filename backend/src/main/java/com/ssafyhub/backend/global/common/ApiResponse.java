package com.ssafyhub.backend.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "공통 API 응답 포맷")
public class ApiResponse<T> {

    @Schema(description = "요청 처리 성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 코드. 성공 시 SUCCESS, 실패 시 도메인별 에러 코드", example = "SUCCESS")
    private String code;

    @Schema(description = "응답 메시지", example = "요청이 정상적으로 처리되었습니다.")
    private String message;

    @Schema(description = "응답 데이터. 없을 경우 null")
    private T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}
