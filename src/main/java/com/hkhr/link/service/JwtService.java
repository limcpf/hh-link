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

    public String fetchToken(String domain) {
        return fetchTokenWithRaw(domain).token;
    }

    public static class JwtFetchResult {
        public final String token;
        public final String rawBody;

        public JwtFetchResult(String token, String rawBody) {
            this.token = token;
            this.rawBody = rawBody;
        }
    }

    public JwtFetchResult fetchTokenWithRaw(String domain) {
        String tokenUrl = settings.getAuthTokenUrl(domain);
        String serviceKey = settings.getAuthServiceKey(domain);
        if (tokenUrl == null || serviceKey == null) {
            throw new IllegalStateException("Missing auth configuration for domain: " + domain);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("servicekey", serviceKey);
        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(tokenUrl, HttpMethod.POST, req, String.class);
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

    private String extractToken(JsonNode json) {
        if (json == null) return null;
        if (json.hasNonNull("token")) return json.get("token").asText();
        if (json.hasNonNull("jwt")) return json.get("jwt").asText();
        if (json.hasNonNull("access_token")) return json.get("access_token").asText();
        // Try nested common fields
        if (json.has("data")) {
            JsonNode d = json.get("data");
            if (d.hasNonNull("token")) return d.get("token").asText();
            if (d.hasNonNull("access_token")) return d.get("access_token").asText();
        }
        return null;
    }
}
