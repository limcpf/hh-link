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

    public static List<String> readUserIds(Path usersJson, ObjectMapper mapper) throws IOException {
        List<String> ids = new ArrayList<String>();
        JsonFactory jf = mapper.getFactory();
        try (JsonParser p = jf.createParser(usersJson.toFile())) {
            if (p.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("users.json must be a top-level array: " + usersJson);
            }
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (p.currentToken() == JsonToken.START_OBJECT) {
                    String id = null;
                    while (p.nextToken() != JsonToken.END_OBJECT) {
                        String field = p.getCurrentName();
                        p.nextToken();
                        if ("userId".equals(field) || "id".equals(field)) { id = p.getValueAsString(); }
                        else { p.skipChildren(); }
                    }
                    if (id != null) ids.add(id);
                } else { p.skipChildren(); }
            }
        }
        return ids;
    }
}

