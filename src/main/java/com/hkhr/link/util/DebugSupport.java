package com.hkhr.link.util;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugSupport {
    public final boolean enabled;
    public final boolean dumpSensitive;
    public final int maxDumps; // -1 means unlimited
    private final AtomicInteger counter = new AtomicInteger(0);
    public final Path baseDir;

    private DebugSupport(boolean enabled, boolean dumpSensitive, int maxDumps, Path baseDir) {
        this.enabled = enabled;
        this.dumpSensitive = dumpSensitive;
        this.maxDumps = maxDumps;
        this.baseDir = baseDir;
    }

    public static DebugSupport from(StepExecution stepExecution, String outputDir) {
        JobParameters p = stepExecution.getJobParameters();
        boolean enabled = parseBool(p.getString("job.debug"), false);
        boolean dumpSensitive = parseBool(p.getString("job.debug.dump-sensitive"), false);
        int maxDumps = parseInt(p.getString("job.debug.max-dumps"), -1);
        String tag = p.getString("requestTime");
        if (tag == null || tag.trim().isEmpty()) {
            tag = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }
        Path base = Paths.get(outputDir, "debug", tag);
        return new DebugSupport(enabled, dumpSensitive, maxDumps, base);
    }

    public boolean shouldDump() {
        if (!enabled) return false;
        if (maxDumps < 0) return true;
        return counter.getAndIncrement() < maxDumps;
    }

    public void write(String relativeFile, String content) {
        if (!enabled) return;
        try {
            Path path = baseDir.resolve(relativeFile);
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Swallow to avoid affecting job; could be logged by caller if desired
        }
    }

    public static String maskToken(String token, boolean dumpSensitive) {
        if (token == null) return "";
        if (dumpSensitive) return token;
        String t = token.trim();
        int n = t.length();
        if (n <= 10) return repeat('*', Math.max(0, n));
        return t.substring(0, 6) + repeat('*', Math.max(0, n - 10)) + t.substring(n - 4);
    }

    public static String maskAuthHeader(String header, boolean dumpSensitive) {
        if (header == null) return "";
        if (dumpSensitive) return header;
        String h = header.trim();
        if (h.toLowerCase().startsWith("bearer ")) {
            String token = h.substring(7);
            return "Bearer " + maskToken(token, false);
        }
        return "*masked*";
    }

    public static String sanitize(String s) {
        if (s == null) return "null";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }

    private static boolean parseBool(String v, boolean def) {
        if (v == null || v.trim().isEmpty()) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static int parseInt(String v, int def) {
        if (v == null || v.trim().isEmpty()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }
}
