package com.ssafyhub.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "팀명을 입력해주세요.")
        @Size(max = 50, message = "팀명은 50자를 초과할 수 없습니다.")
        String teamName,

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotBlank(message = "학번을 입력해주세요.")
        @Size(max = 30, message = "학번은 30자를 초과할 수 없습니다.")
        String studentNumber,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
        String password,

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
