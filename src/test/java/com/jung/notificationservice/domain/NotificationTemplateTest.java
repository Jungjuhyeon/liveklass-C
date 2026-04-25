package com.jung.notificationservice.domain;

import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateTest {

    @Test
    @DisplayName("템플릿 변수가 올바르게 치환된다")
    void resolve_replacesVariables() {
        NotificationTemplate template = NotificationTemplate.create(
                "ENROLLMENT_COMPLETE",
                NotificationChannel.EMAIL,
                "{lectureName} 수강 신청 완료",
                "안녕하세요 {userName}님, {lectureName} 강의 수강 신청이 완료되었습니다."
        );

        Map<String, String> variables = Map.of("lectureName", "스프링 부트", "userName", "홍길동");

        assertThat(template.resolveTitle(variables)).isEqualTo("스프링 부트 수강 신청 완료");
        assertThat(template.resolveBody(variables)).isEqualTo("안녕하세요 홍길동님, 스프링 부트 강의 수강 신청이 완료되었습니다.");
    }

    @Test
    @DisplayName("변수가 없으면 템플릿 원문이 반환된다")
    void resolve_noVariables_returnsOriginal() {
        NotificationTemplate template = NotificationTemplate.create(
                "NOTICE",
                NotificationChannel.IN_APP,
                "공지사항",
                "새로운 공지사항이 등록되었습니다."
        );

        assertThat(template.resolveTitle(Map.of())).isEqualTo("공지사항");
        assertThat(template.resolveBody(Map.of())).isEqualTo("새로운 공지사항이 등록되었습니다.");
    }

    @Test
    @DisplayName("템플릿에 없는 변수는 치환되지 않고 남는다")
    void resolve_unknownVariable_remainsAsIs() {
        NotificationTemplate template = NotificationTemplate.create(
                "ENROLLMENT_COMPLETE",
                NotificationChannel.EMAIL,
                "{lectureName} 수강 신청 완료",
                "{unknown} 변수"
        );

        Map<String, String> variables = Map.of("lectureName", "스프링 부트");

        assertThat(template.resolveTitle(variables)).isEqualTo("스프링 부트 수강 신청 완료");
        assertThat(template.resolveBody(variables)).isEqualTo("{unknown} 변수");
    }
}
