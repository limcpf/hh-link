package com.hkhr.link.util;

import org.springframework.web.client.RestClientResponseException;

// 디버그 덤프 보조 유틸: 공통 요청/응답/에러 포맷으로 파일 기록
public class DebugDumpUtils {

    public static void dumpListRequest(DebugSupport debug, String domainKey, String url, String authMasked, String payload) {
        if (!debug.enabled || !debug.shouldDump()) return;
        String req = "{\n" +
                "  \"method\": \"POST\",\n" +
                "  \"url\": \"" + url + "\",\n" +
                "  \"authorization\": \"" + authMasked + "\",\n" +
                "  \"payload\": " + (payload == null ? "{}" : payload) + "\n" +
                "}";
        debug.write("api/" + domainKey + "/req-list.json", req);
    }

    public static void dumpListResponse(DebugSupport debug, String domainKey, String body) {
        if (!debug.enabled || !debug.shouldDump()) return;
        debug.write("api/" + domainKey + "/resp-list.json", body == null ? "" : body);
    }

    public static void dumpListError(DebugSupport debug, String domainKey, String url, String authMasked, String payload, Exception e) {
        if (!debug.enabled) return;
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR list call\n");
        sb.append("url=").append(url).append('\n');
        sb.append("auth=").append(authMasked).append('\n');
        sb.append("payload=").append(payload).append('\n');
        appendHttpDetails(sb, e);
        sb.append("stacktrace= - 스택트레이스\n").append(DebugSupport.stackTrace(e));
        debug.write("api/" + domainKey + "/error-list.txt", sb.toString());
    }

    public static void dumpByUserRequest(DebugSupport debug, String domainKey, String userId, String url, String authMasked, String payload) {
        if (!debug.enabled || !debug.shouldDump()) return;
        String req = "{\n" +
                "  \"method\": \"POST\",\n" +
                "  \"url\": \"" + url + "\",\n" +
                "  \"userId\": \"" + DebugSupport.sanitize(userId) + "\",\n" +
                "  \"authorization\": \"" + authMasked + "\",\n" +
                "  \"payload\": " + (payload == null ? "{}" : payload) + "\n" +
                "}";
        String fname = "api/" + domainKey + "/req-by-user-" + DebugSupport.sanitize(userId) + ".json";
        debug.write(fname, req);
    }

    public static void dumpByUserResponse(DebugSupport debug, String domainKey, String userId, String body) {
        if (!debug.enabled || !debug.shouldDump()) return;
        String fname = "api/" + domainKey + "/resp-by-user-" + DebugSupport.sanitize(userId) + ".json";
        debug.write(fname, body == null ? "" : body);
    }

    public static void dumpByUserError(DebugSupport debug, String domainKey, String userId, String url, String authMasked, String payload, Exception e) {
        if (!debug.enabled) return;
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR by-user call\n");
        sb.append("url=").append(url).append('\n');
        sb.append("userId=").append(userId).append('\n');
        sb.append("auth=").append(authMasked).append('\n');
        sb.append("payload=").append(payload).append('\n');
        appendHttpDetails(sb, e);
        sb.append("stacktrace= - 스택트레이스\n").append(DebugSupport.stackTrace(e));
        String fname = "api/" + domainKey + "/error-by-user-" + DebugSupport.sanitize(userId) + ".txt";
        debug.write(fname, sb.toString());
    }

    public static void dumpJwtError(DebugSupport debug, String domainKey, String tokenUrl, String serviceKeyMasked, Exception e) {
        if (!debug.enabled) return;
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR jwt fetch\n");
        sb.append("tokenUrl=").append(tokenUrl).append('\n');
        sb.append("serviceKey=").append(serviceKeyMasked).append('\n');
        appendHttpDetails(sb, e);
        sb.append("stacktrace= - 스택트레이스\n").append(DebugSupport.stackTrace(e));
        debug.write("jwt/error-jwt-" + domainKey + ".txt", sb.toString());
    }

    private static void appendHttpDetails(StringBuilder sb, Exception e) {
        if (e instanceof RestClientResponseException) {
            RestClientResponseException r = (RestClientResponseException) e;
            sb.append("status=").append(r.getRawStatusCode()).append('\n');
            sb.append("responseBody=").append(r.getResponseBodyAsString()).append('\n');
        }
    }
}

