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

// 잡 파라미터 기반 디버깅 지원 유틸리티
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

    // StepExecution에서 잡 파라미터를 읽어 DebugSupport 인스턴스를 생성합니다.
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

    // 최대 덤프 횟수(maxDumps)가 설정된 경우, 해당 횟수 이내에서만 true를 반환합니다.
    public boolean shouldDump() {
        if (!enabled) return false;
        if (maxDumps < 0) return true;
        return counter.getAndIncrement() < maxDumps;
    }

    // 지정한 상대 경로로 내용을 UTF-8로 저장합니다(실패 시 배치에 영향 없도록 무시).
    public void write(String relativeFile, String content) {
        if (!enabled) return;
        try {
            Path path = baseDir.resolve(relativeFile);
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) { /* 배치에 영향 방지용으로 무시(필요 시 호출부 로깅) */ }
    }

    // 토큰 마스킹 도우미(앞 6, 뒤 4만 남기고 가운데 * 처리)
    public static String maskToken(String token, boolean dumpSensitive) {
        if (token == null) return "";
        if (dumpSensitive) return token;
        String t = token.trim();
        int n = t.length();
        if (n <= 10) return repeat('*', Math.max(0, n));
        return t.substring(0, 6) + repeat('*', Math.max(0, n - 10)) + t.substring(n - 4);
    }

    // Authorization 헤더 마스킹(Bearer 토큰 대상)
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
