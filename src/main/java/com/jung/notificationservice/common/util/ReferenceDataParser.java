package com.jung.notificationservice.common.util;


import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

@Slf4j
public class ReferenceDataParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ReferenceDataParser() {}

    public static Map<String, String> parse(String referenceData) {
        if (referenceData == null || referenceData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(referenceData, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("referenceData 파싱 실패 - data={}", referenceData);
            return Collections.emptyMap();
        }
    }
}
