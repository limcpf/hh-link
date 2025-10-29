package com.hkhr.link.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonArrayFileWriterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void writesEmptyArray(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("empty.json");
        try (JsonArrayFileWriter w = new JsonArrayFileWriter(out, false)) {
            // no elements
        }
        String content = new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(content);
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(0);
    }

    @Test
    void writesSingleElement(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("one.json");
        try (JsonArrayFileWriter w = new JsonArrayFileWriter(out, false)) {
            w.writeNode(mapper.readTree("{\"a\":1}"));
        }
        JsonNode node = mapper.readTree(new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(1);
        assertThat(node.get(0).get("a").asInt()).isEqualTo(1);
    }

    @Test
    void writesMultipleElements(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("many.json");
        try (JsonArrayFileWriter w = new JsonArrayFileWriter(out, false)) {
            w.writeNode(mapper.readTree("{\"i\":1}"));
            w.writeNode(mapper.readTree("{\"i\":2}"));
            w.writeNode(mapper.readTree("{\"i\":3}"));
        }
        JsonNode node = mapper.readTree(new String(Files.readAllBytes(out), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(3);
        assertThat(node.get(2).get("i").asInt()).isEqualTo(3);
    }

    @Test
    void writesRawJsonElement(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("raw.json");
        try (JsonArrayFileWriter w = new JsonArrayFileWriter(out, false)) {
            w.writeRawJsonElement("{\"x\":\"y\"}");
        }
        JsonNode node = mapper.readTree(Files.readString(out));
        assertThat(node.size()).isEqualTo(1);
        assertThat(node.get(0).get("x").asText()).isEqualTo("y");
    }
}
