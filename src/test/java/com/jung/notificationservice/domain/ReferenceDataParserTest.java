package com.jung.notificationservice.domain;

import com.jung.notificationservice.common.util.ReferenceDataParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataParserTest {

    @Test
    @DisplayName("JSON 문자열을 Map으로 파싱한다")
    void parse_validJson_returnsMap() {
        String json = "{\"lectureName\":\"스프링 부트\",\"userName\":\"홍길동\"}";

        Map<String, String> result = ReferenceDataParser.parse(json);

        assertThat(result).containsEntry("lectureName", "스프링 부트")
                          .containsEntry("userName", "홍길동");
    }

    @Test
    @DisplayName("null이면 빈 Map을 반환한다")
    void parse_null_returnsEmptyMap() {
        assertThat(ReferenceDataParser.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열이면 빈 Map을 반환한다")
    void parse_blank_returnsEmptyMap() {
        assertThat(ReferenceDataParser.parse("  ")).isEmpty();
    }

    @Test
    @DisplayName("올바르지 않은 JSON이면 빈 Map을 반환한다")
    void parse_invalidJson_returnsEmptyMap() {
        assertThat(ReferenceDataParser.parse("not-json")).isEmpty();
    }
}
