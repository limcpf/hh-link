package com.hkhr.link.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// 대용량에도 메모리 사용을 최소화하기 위해 JSON 배열을 스트리밍으로 파일에 쓰는 유틸리티
public class JsonArrayFileWriter implements Closeable {
    private final ObjectMapper mapper;
    private final JsonGenerator generator;
    private boolean first = true;

    // 파일을 열고 JSON 배열([) 시작 토큰을 작성합니다. pretty=true면 보기 좋게 출력합니다.
    public JsonArrayFileWriter(Path path, boolean pretty) throws IOException {
        this.mapper = new ObjectMapper();
        Files.createDirectories(path.getParent());
        File file = path.toFile();
        FileOutputStream fos = new FileOutputStream(file, false);
        JsonFactory factory = mapper.getFactory();
        this.generator = factory.createGenerator(fos, com.fasterxml.jackson.core.JsonEncoding.UTF8);
        if (pretty) {
            generator.useDefaultPrettyPrinter();
        }
        generator.writeStartArray();
    }

    // 개별 JSON 노드를 배열의 요소로 추가합니다.
    public synchronized void writeNode(JsonNode node) throws IOException {
        // 주의: JsonGenerator가 배열 컨텍스트에서 콤마 구분을 자동 처리합니다.
        generator.writeTree(node);
    }

    // 문자열 JSON을 파싱하여 배열 요소로 추가합니다.
    public synchronized void writeRawJsonElement(String json) throws IOException {
        // 바인딩 없이 원시 JSON을 쓸 때 사용합니다.
        if (json == null || json.trim().isEmpty()) return;
        // 유효한 JSON인지 확인하기 위해 파싱 후 기록합니다.
        JsonNode n = mapper.readTree(json);
        writeNode(n);
    }

    @Override
    // 배열의 끝(])을 기록하고 리소스를 정리합니다.
    public synchronized void close() throws IOException {
        generator.writeEndArray();
        generator.flush();
        generator.close();
    }
}
