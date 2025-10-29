package com.hkhr.link.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

// HTTP JSON 호출 보조 유틸(경량)
public class HttpJson {
    public static HttpHeaders authJsonHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    public static ResponseEntity<String> post(RestTemplate rt, String url, HttpHeaders headers, String payload) {
        return rt.postForEntity(url, new HttpEntity<String>(payload, headers), String.class);
    }

    // JWT 발급 등 바디 없이 헤더만 보내는 POST 호출
    public static HttpHeaders serviceKeyHeaders(String serviceKey) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("servicekey", serviceKey);
        return h;
    }

    public static ResponseEntity<String> postNoBody(RestTemplate rt, String url, HttpHeaders headers) {
        return rt.exchange(url, org.springframework.http.HttpMethod.POST, new HttpEntity<Void>(headers), String.class);
    }
}
