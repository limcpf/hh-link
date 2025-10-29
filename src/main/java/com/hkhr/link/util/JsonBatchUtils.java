package com.hkhr.link.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// JSON 처리 보조 유틸(배치 사용)
public class JsonBatchUtils {
    public static long appendBody(JsonArrayFileWriter writer, ObjectMapper mapper, String body) throws IOException {
        if (body == null || body.trim().isEmpty()) return 0L;
        JsonNode node = mapper.readTree(body);
        long added = 0L;
        if (node.isArray()) {
            for (JsonNode e : node) { writer.writeNode(e); added++; }
        } else { writer.writeNode(node); added = 1L; }
        return added;
    }

    // readUserIds는 더 이상 사용하지 않습니다(단일 API 호출로 통일).
}
