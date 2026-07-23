package com.ssafyhub.backend.domain.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SignupRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    private SignupRequest validRequest() {
        return new SignupRequest("team-a", "홍길동", "202512345", "password1234", "password1234");
    }

    @Test
    void 모든_값이_유효하면_위반이_없다() {
        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void 팀명이_비어있으면_위반이_발생한다() {
        SignupRequest request = new SignupRequest("", "홍길동", "202512345", "password1234", "password1234");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("teamName");
    }

    @Test
    void 학번이_비어있으면_위반이_발생한다() {
        SignupRequest request = new SignupRequest("team-a", "홍길동", "", "password1234", "password1234");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("studentNumber");
    }

    @Test
    void 비밀번호가_8자_미만이면_위반이_발생한다() {
        SignupRequest request = new SignupRequest("team-a", "홍길동", "202512345", "short1", "short1");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
    }

    @Test
    void 비밀번호_확인이_8자_미만이면_위반이_발생한다() {
        SignupRequest request = new SignupRequest("team-a", "홍길동", "202512345", "password1234", "short1");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("passwordConfirm");
    }
}
