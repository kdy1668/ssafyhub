package com.ssafyhub.backend.global.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafyhub.backend.domain.deployment.dto.FailureDiagnosis;
import com.ssafyhub.backend.domain.deployment.dto.RuntimeLogInsight;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 손으로 적은 JSON 스키마가 record 정의와 어긋나지 않는지 검증한다.
 *
 * 스키마는 모델에게 보낼 형식이고 record는 그 응답을 담을 그릇이다. 둘이 어긋나면
 * 실제 호출에서야 SCHEMA_MISMATCH로 터지는데, API 키가 필요해 테스트로 잡기 어렵다.
 * 그래서 필드 이름 수준의 정합성만이라도 여기서 막는다.
 *
 * strict 모드 요구사항(additionalProperties: false, 전 필드 required)도 함께 확인한다.
 * 하나라도 빠지면 OpenAI가 400을 돌려주는데, 그 역시 호출해봐야 알 수 있다.
 */
class AiJsonSchemaConsistencyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<Arguments> schemas() {
        return Stream.of(
                Arguments.of("failure_diagnosis", FailureDiagnosis.SCHEMA),
                Arguments.of("runtime_log_insight", RuntimeLogInsight.SCHEMA));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("스키마가 유효한 JSON이고 이름이 일치한다")
    @MethodSource("schemas")
    void schemaIsValidJson(String expectedName, AiJsonSchema<?> schema) throws Exception {
        assertThat(schema.name()).isEqualTo(expectedName);

        JsonNode root = MAPPER.readTree(schema.schema());
        assertThat(root.path("type").asText()).isEqualTo("object");
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("최상위 스키마의 필드 이름이 record 컴포넌트와 정확히 일치한다")
    @MethodSource("schemas")
    void topLevelFieldsMatchRecordComponents(String name, AiJsonSchema<?> schema) throws Exception {
        JsonNode properties = MAPPER.readTree(schema.schema()).path("properties");

        List<String> schemaFields = new ArrayList<>();
        properties.fieldNames().forEachRemaining(schemaFields::add);

        List<String> recordFields = Arrays.stream(schema.type().getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertThat(schemaFields).containsExactlyInAnyOrderElementsOf(recordFields);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("strict 모드 요구사항을 모든 object 노드가 만족한다")
    @MethodSource("schemas")
    void satisfiesStrictModeRequirements(String name, AiJsonSchema<?> schema) throws Exception {
        assertStrict(MAPPER.readTree(schema.schema()), name);
    }

    /**
     * strict 모드는 모든 object에 additionalProperties: false를 요구하고,
     * properties의 모든 키가 required에 있어야 한다. 재귀적으로 확인한다.
     */
    private void assertStrict(JsonNode node, String path) {
        if (!node.isObject()) {
            return;
        }

        if ("object".equals(node.path("type").asText())) {
            assertThat(node.has("additionalProperties"))
                    .as("%s: additionalProperties 선언이 필요하다", path)
                    .isTrue();
            assertThat(node.path("additionalProperties").asBoolean(true))
                    .as("%s: additionalProperties는 false여야 한다", path)
                    .isFalse();

            List<String> properties = new ArrayList<>();
            node.path("properties").fieldNames().forEachRemaining(properties::add);

            List<String> required = new ArrayList<>();
            node.path("required").forEach(item -> required.add(item.asText()));

            assertThat(required)
                    .as("%s: properties의 모든 키가 required에 있어야 한다", path)
                    .containsExactlyInAnyOrderElementsOf(properties);
        }

        // 중첩 object(배열 items 포함)까지 내려간다.
        node.path("properties").fields().forEachRemaining(
                entry -> assertStrict(entry.getValue(), path + "." + entry.getKey()));
        if (node.has("items")) {
            assertStrict(node.path("items"), path + "[]");
        }
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("enum 값이 record의 enum 상수와 일치한다")
    @MethodSource("schemas")
    void enumValuesMatchJavaEnums(String name, AiJsonSchema<?> schema) throws Exception {
        JsonNode properties = MAPPER.readTree(schema.schema()).path("properties");

        for (var component : schema.type().getRecordComponents()) {
            if (!component.getType().isEnum()) {
                continue;
            }
            JsonNode field = properties.path(component.getName());
            List<String> schemaValues = new ArrayList<>();
            field.path("enum").forEach(item -> schemaValues.add(item.asText()));

            List<String> javaValues = Arrays.stream(component.getType().getEnumConstants())
                    .map(constant -> ((Enum<?>) constant).name())
                    .toList();

            assertThat(schemaValues)
                    .as("%s.%s enum 값", name, component.getName())
                    .containsExactlyInAnyOrderElementsOf(javaValues);
        }
    }
}
