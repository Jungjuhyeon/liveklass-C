package com.jung.notificationservice.domain;

import com.jung.notificationservice.common.domain.BaseEntity;
import com.jung.notificationservice.domain.enumeration.NotificationChannel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"notification_type", "channel"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String titleTemplate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    private NotificationTemplate(String notificationType, NotificationChannel channel,
                                  String titleTemplate, String bodyTemplate) {
        this.notificationType = notificationType;
        this.channel = channel;
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public static NotificationTemplate create(String notificationType, NotificationChannel channel,
                                               String titleTemplate, String bodyTemplate) {
        return new NotificationTemplate(notificationType, channel, titleTemplate, bodyTemplate);
    }

    public String resolveTitle(java.util.Map<String, String> variables) {
        return resolve(titleTemplate, variables);
    }

    public String resolveBody(java.util.Map<String, String> variables) {
        return resolve(bodyTemplate, variables);
    }

    private String resolve(String template, java.util.Map<String, String> variables) {
        String result = template;
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
