package com.hkhr.link.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkhr.link.config.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

// 도메인별 JWT 발급 서비스
@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final RestTemplate restTemplate;
    private final AppSettings settings;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(RestTemplate restTemplate, AppSettings settings) {
        this.restTemplate = restTemplate;
        this.settings = settings;
    }

    // JWT만 필요할 때 간편 호출용 래퍼
    public String fetchToken(String domain) {
        return fetchTokenWithRaw(domain).token;
    }

    // 토큰과 원문 응답을 함께 보관(디버그 덤프 목적)
    public static class JwtFetchResult {
        public final String token;
        public final String rawBody;

        public JwtFetchResult(String token, String rawBody) {
            this.token = token;
            this.rawBody = rawBody;
        }
    }

    // servicekey 헤더를 포함하여 POST로 토큰을 발급받고, 원문과 추출된 토큰을 반환
    public JwtFetchResult fetchTokenWithRaw(String domain) {
        String tokenUrl = settings.getAuthTokenUrl(domain);
        String serviceKey = settings.getAuthServiceKey(domain);
        if (tokenUrl == null || serviceKey == null) {
            throw new IllegalStateException("Missing auth configuration for domain: " + domain);
        }

        HttpHeaders headers = com.hkhr.link.util.HttpJson.serviceKeyHeaders(serviceKey);

        try {
            ResponseEntity<String> resp = com.hkhr.link.util.HttpJson.postNoBody(restTemplate, tokenUrl, headers);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("Token endpoint non-2xx for domain " + domain + ": " + resp.getStatusCode());
            }
            String raw = resp.getBody();
            JsonNode json = objectMapper.readTree(raw);
            String token = extractToken(json);
            if (token == null || token.isEmpty()) {
                throw new IllegalStateException("Token not found in response for domain " + domain);
            }
            log.info("Fetched JWT for domain {}", domain);
            return new JwtFetchResult(token, raw);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Token request failed for domain " + domain + ": " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Token request failed for domain " + domain + ": " + e.getMessage(), e);
        }
    }

    // 응답 JSON에서 토큰 값을 추출합니다. 우선순위: jwt > token > access_token > data.*
    private String extractToken(JsonNode json) {
        if (json == null) return null;
        if (json.hasNonNull("token")) return json.get("token").asText();
        if (json.hasNonNull("jwt")) return json.get("jwt").asText();
        if (json.hasNonNull("access_token")) return json.get("access_token").asText();
        // 중첩 필드(data.*)에서도 검색
        if (json.has("data")) {
            JsonNode d = json.get("data");
            if (d.hasNonNull("token")) return d.get("token").asText();
            if (d.hasNonNull("access_token")) return d.get("access_token").asText();
        }
        return null;
    }
}
