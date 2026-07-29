package com.ssafyhub.backend.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignupRequest(
        @Schema(description = "팀명", example = "SSAFYHUB")
        @NotBlank(message = "팀명을 입력해주세요.")
        @Size(max = 50, message = "팀명은 50자를 초과할 수 없습니다.")
        String teamName,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "학번 (숫자 7자리)", example = "1234567")
        @NotBlank(message = "학번을 입력해주세요.")
        @Pattern(regexp = "^\\d{7}$", message = "학번은 숫자 7자리로 입력해주세요.")
        String studentNumber,

        @Schema(description = "비밀번호 (8~100자)", example = "password123")
        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
        String password,

        @Schema(description = "비밀번호 확인", example = "password123")
        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호 확인은 8자 이상 100자 이하로 입력해주세요.")
        String passwordConfirm
) {
    @Override
    public String toString() {
        return "SignupRequest[teamName=%s, name=%s, studentNumber=%s, password=****, passwordConfirm=****]"
                .formatted(teamName, name, studentNumber);
    }
}
