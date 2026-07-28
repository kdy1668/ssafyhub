package com.ssafyhub.backend.global.config.swagger;

import com.ssafyhub.backend.global.error.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

/**
 * {@link ApiErrorCodeExample}로 선언된 {@link ErrorCode}들을 HTTP 상태 코드별로 묶어
 * Swagger 응답 예시로 자동 추가한다.
 */
@Component
public class ErrorCodeOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        List<ErrorCode> errorCodes = resolveErrorCodes(handlerMethod);

        if (errorCodes.isEmpty()) {
            return operation;
        }

        ApiResponses responses = operation.getResponses();

        Map<String, List<ErrorCode>> groupedByStatus = errorCodes.stream()
                .collect(Collectors.groupingBy(
                        errorCode -> String.valueOf(errorCode.getStatus().value()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        groupedByStatus.forEach((status, codes) -> responses.addApiResponse(status, buildResponse(codes)));

        return operation;
    }

    private List<ErrorCode> resolveErrorCodes(HandlerMethod handlerMethod) {
        ApiErrorCodeExamples container = handlerMethod.getMethodAnnotation(ApiErrorCodeExamples.class);
        if (container != null) {
            return Arrays.stream(container.value())
                    .map(ApiErrorCodeExample::value)
                    .toList();
        }

        ApiErrorCodeExample single = handlerMethod.getMethodAnnotation(ApiErrorCodeExample.class);
        if (single != null) {
            return List.of(single.value());
        }

        return List.of();
    }

    private ApiResponse buildResponse(List<ErrorCode> errorCodes) {
        MediaType mediaType = new MediaType();
        errorCodes.forEach(errorCode -> mediaType.addExamples(errorCode.getCode(), toExample(errorCode)));

        Content content = new Content().addMediaType("application/json", mediaType);

        return new ApiResponse()
                .description(errorCodes.get(0).getStatus().getReasonPhrase())
                .content(content);
    }

    private Example toExample(ErrorCode errorCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());
        body.put("data", null);

        return new Example()
                .summary(errorCode.getMessage())
                .value(body);
    }
}
