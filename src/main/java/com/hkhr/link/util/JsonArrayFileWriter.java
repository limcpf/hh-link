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

public class JsonArrayFileWriter implements Closeable {
    private final ObjectMapper mapper;
    private final JsonGenerator generator;
    private boolean first = true;

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

    public synchronized void writeNode(JsonNode node) throws IOException {
        // JsonGenerator handles comma separation when writing elements in array context.
        generator.writeTree(node);
    }

    public synchronized void writeRawJsonElement(String json) throws IOException {
        // For raw JSON elements when we don't want to bind
        if (json == null || json.trim().isEmpty()) return;
        // Parse to ensure valid JSON and then write
        JsonNode n = mapper.readTree(json);
        writeNode(n);
    }

    @Override
    public synchronized void close() throws IOException {
        generator.writeEndArray();
        generator.flush();
        generator.close();
    }
}

