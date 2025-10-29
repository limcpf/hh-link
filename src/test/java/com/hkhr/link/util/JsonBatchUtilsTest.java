package com.hkhr.link.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonBatchUtilsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void appendBody_addsArrayOrObject(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("out.json");
        try (JsonArrayFileWriter w = new JsonArrayFileWriter(out, false)) {
            long a = JsonBatchUtils.appendBody(w, mapper, "[{\"a\":1},{\"b\":2}]");
            long b = JsonBatchUtils.appendBody(w, mapper, "{\"c\":3}");
            assertThat(a).isEqualTo(2);
            assertThat(b).isEqualTo(1);
        }
        JsonNode node = mapper.readTree(new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(3);
    }

    @Test
    void readUserIds_readsIds(@TempDir Path tmp) throws Exception {
        Path users = tmp.resolve("users.json");
        Files.write(users, "[{\"userId\":\"U1\"},{\"id\":\"U2\"}]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        List<String> ids = JsonBatchUtils.readUserIds(users, mapper);
        assertThat(ids).containsExactly("U1", "U2");
    }
}

