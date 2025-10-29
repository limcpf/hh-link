package com.hkhr.link.tasklet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkhr.link.config.AppSettings;
import com.hkhr.link.domain.Domain;
import com.hkhr.link.util.JsonArrayFileWriter;
import com.hkhr.link.util.DebugSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.util.Date;

// 공통 재사용 Tasklet
// - 독립 도메인: list-url로 POST + JSON 요청 1회 → 결과를 <domain>s.json으로 저장
// - 종속 도메인: users.json의 userId를 순회하며 POST 호출 → 모든 결과를 하나의 배열로 누적 저장
public class FetchAndSaveJsonTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(FetchAndSaveJsonTasklet.class);

    private final Domain domain;
    private final AppSettings settings;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public FetchAndSaveJsonTasklet(Domain domain, AppSettings settings, RestTemplate restTemplate) {
        this.domain = domain;
        this.settings = settings;
        this.restTemplate = restTemplate;
    }

    @Override
    // 실제 실행 로직: JWT를 읽어 헤더에 설정하고, 도메인 유형에 따라 호출/저장을 수행합니다.
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        DebugSupport debug = DebugSupport.from(stepExecution, settings.getOutputDir());
        String token = stepExecution.getJobExecution()
                .getExecutionContext().getString("jwt." + domain.key(), null);
        if (token == null) {
            throw new IllegalStateException("JWT for domain '" + domain + "' was not found in context. Ensure FetchJWT step ran before.");
        }

        // 파일명 접미사: YYYYMMDD (requestTime 있으면 해당 날짜, 없으면 오늘)
        String requestTime = stepExecution.getJobParameters().getString("requestTime");
        String date = (requestTime != null && requestTime.length() >= 8) ? requestTime.substring(0, 8)
                : new SimpleDateFormat("yyyyMMdd").format(new Date());
        String outFileName = domain.plural() + "-" + date + ".json";
        Path outPath = Paths.get(settings.getOutputDir(), outFileName);

        if (Files.exists(outPath) && !settings.isOverwrite()) {
            // 안전을 위해 기본은 덮어쓰지 않습니다.
            throw new IllegalStateException("Output file already exists (overwrite=false): " + outPath);
        }

        Files.createDirectories(outPath.getParent());

        long start = System.currentTimeMillis();
        long totalItems = 0L;
        long failures = 0L;

        try (JsonArrayFileWriter writer = new JsonArrayFileWriter(outPath, settings.isPretty())) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            if (domain.isIndependent()) {
                // 독립 도메인: 1회 POST 호출
                String listUrl = settings.getListUrl(domain);
                if (listUrl == null) throw new IllegalStateException("Missing endpoints." + domain.key() + ".list-url");
                String payload = settings.getRequestPayload(domain);
                if (payload == null || payload.trim().isEmpty()) payload = "{}";
                if (debug.enabled && debug.shouldDump()) {
                    String reqMeta = "{\n" +
                            "  \"method\": \"POST\",\n" +
                            "  \"url\": \"" + listUrl + "\",\n" +
                            "  \"authorization\": \"" + DebugSupport.maskAuthHeader("Bearer " + token, debug.dumpSensitive) + "\",\n" +
                            "  \"payload\": " + payload + "\n" +
                            "}";
                    debug.write("api/" + domain.key() + "/req-list.json", reqMeta);
                }
                try {
                    ResponseEntity<String> resp = restTemplate.exchange(listUrl, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
                    totalItems = appendResponseBody(writer, resp.getBody());
                    log.info("{}: fetched {} items from {}", domain.key(), totalItems, listUrl);
                    if (debug.enabled && debug.shouldDump()) {
                        debug.write("api/" + domain.key() + "/resp-list.json", resp.getBody() == null ? "" : resp.getBody());
                    }
                } catch (Exception e) {
                    if (debug.enabled) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("ERROR list call\n");
                        sb.append("url=").append(listUrl).append('\n');
                        sb.append("auth=").append(DebugSupport.maskAuthHeader("Bearer " + token, debug.dumpSensitive)).append('\n');
                        sb.append("payload=").append(payload).append('\n');
                        if (e instanceof org.springframework.web.client.RestClientResponseException) {
                            org.springframework.web.client.RestClientResponseException r = (org.springframework.web.client.RestClientResponseException)e;
                            sb.append("status=").append(r.getRawStatusCode()).append('\n');
                            sb.append("responseBody=").append(r.getResponseBodyAsString()).append('\n');
                        }
                        sb.append("stacktrace=\n").append(DebugSupport.stackTrace(e));
                        debug.write("api/" + domain.key() + "/error-list.txt", sb.toString());
                    }
                    throw e;
                }
            } else {
                // 종속 도메인: users.json 기반으로 사용자 단위 반복 호출(옵션 병렬)
                // users-YYYYMMDD.json 경로를 동일한 날짜 규칙으로 계산
                Path usersPath = Paths.get(settings.getOutputDir(), Domain.USER.plural() + "-" + date + ".json");
                if (!Files.exists(usersPath)) {
                    throw new IllegalStateException("Dependent domain requires users.json. Not found: " + usersPath);
                }
                List<String> userIds = readUserIds(usersPath);
                log.info("{}: will fetch by {} userIds (threads={})", domain.key(), userIds.size(), settings.getMaxThreads());

                ExecutorService pool = settings.getMaxThreads() > 1
                        ? Executors.newFixedThreadPool(settings.getMaxThreads())
                        : Executors.newSingleThreadExecutor();
                List<Future<Long>> futures = new ArrayList<>();
                for (String userId : userIds) {
                    futures.add(pool.submit(() -> {
                        String tpl = settings.getByUserUrlTemplate(domain);
                        if (tpl == null) throw new IllegalStateException("Missing endpoints." + domain.key() + ".by-user-url-template");
                        String url = tpl.replace("{userId}", urlEncode(userId));
                        String payloadTpl = settings.getByUserPayloadTemplate(domain);
                        String payload = payloadTpl != null && !payloadTpl.trim().isEmpty() ? payloadTpl : "{\"userId\":\"{userId}\"}";
                        payload = payload.replace("{userId}", escapeJson(userId));
                        try {
                            if (debug.enabled && debug.shouldDump()) {
                                String reqMeta = "{\n" +
                                        "  \"method\": \"POST\",\n" +
                                        "  \"url\": \"" + url + "\",\n" +
                                        "  \"userId\": \"" + DebugSupport.sanitize(userId) + "\",\n" +
                                        "  \"authorization\": \"" + DebugSupport.maskAuthHeader("Bearer " + token, debug.dumpSensitive) + "\",\n" +
                                        "  \"payload\": " + payload + "\n" +
                                        "}";
                                String fname = "api/" + domain.key() + "/req-by-user-" + DebugSupport.sanitize(userId) + ".json";
                                debug.write(fname, reqMeta);
                            }
                            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
                            long added = appendResponseBody(writer, resp.getBody());
                            if (debug.enabled && debug.shouldDump()) {
                                String fname = "api/" + domain.key() + "/resp-by-user-" + DebugSupport.sanitize(userId) + ".json";
                                debug.write(fname, resp.getBody() == null ? "" : resp.getBody());
                            }
                            return added;
                        } catch (Exception e) {
                            if (debug.enabled) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("ERROR by-user call\n");
                                sb.append("url=").append(url).append('\n');
                                sb.append("userId=").append(userId).append('\n');
                                sb.append("auth=").append(DebugSupport.maskAuthHeader("Bearer " + token, debug.dumpSensitive)).append('\n');
                                sb.append("payload=").append(payload).append('\n');
                                if (e instanceof org.springframework.web.client.RestClientResponseException) {
                                    org.springframework.web.client.RestClientResponseException r = (org.springframework.web.client.RestClientResponseException)e;
                                    sb.append("status=").append(r.getRawStatusCode()).append('\n');
                                    sb.append("responseBody=").append(r.getResponseBodyAsString()).append('\n');
                                }
                                sb.append("stacktrace=\n").append(DebugSupport.stackTrace(e));
                                String fname = "api/" + domain.key() + "/error-by-user-" + DebugSupport.sanitize(userId) + ".txt";
                                debug.write(fname, sb.toString());
                            }
                            if (settings.isContinueOnError()) {
                                log.warn("{}: userId={} failed: {}", domain.key(), userId, e.getMessage());
                                return 0L;
                            }
                            throw e;
                        }
                    }));
                }

                pool.shutdown();
                for (Future<Long> f : futures) {
                    try {
                        Long added = f.get();
                        if (added != null) totalItems += added;
                    } catch (ExecutionException ee) {
                        failures++;
                        pool.shutdownNow();
                        if (!settings.isContinueOnError()) {
                            throw new IllegalStateException("Fetching failed: " + ee.getCause().getMessage(), ee.getCause());
                        } else {
                            log.warn("{}: task failed but continuing: {}", domain.key(), ee.getCause().getMessage());
                        }
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("{}: done. totalItems={}, failures={}, elapsedMs={}", domain.key(), totalItems, failures, elapsed);
        return RepeatStatus.FINISHED;
    }

    private String escapeJson(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // 응답 본문을 파싱하여 배열 요소로 추가(배열이면 모든 요소, 객체면 단일 요소)
    private long appendResponseBody(JsonArrayFileWriter writer, String body) throws IOException {
        if (body == null || body.trim().isEmpty()) return 0L;
        JsonNode node = mapper.readTree(body);
        long added = 0L;
        if (node.isArray()) {
            for (JsonNode e : node) {
                writer.writeNode(e);
                added++;
            }
        } else {
            writer.writeNode(node);
            added = 1L;
        }
        return added;
    }

    private String urlEncode(String v) {
        return URLEncoder.encode(Objects.toString(v, ""), StandardCharsets.UTF_8);
    }

    // users.json(최상위 배열)에서 userId 또는 id 필드를 추출합니다.
    private List<String> readUserIds(Path usersJson) throws IOException {
        List<String> ids = new ArrayList<>();
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
                        if ("userId".equals(field) || "id".equals(field)) {
                            id = p.getValueAsString();
                        } else {
                            p.skipChildren();
                        }
                    }
                    if (id != null) ids.add(id);
                } else {
                    p.skipChildren();
                }
            }
        }
        return ids;
    }
}
