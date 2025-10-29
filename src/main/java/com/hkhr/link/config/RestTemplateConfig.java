package com.hkhr.link.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// RestTemplate 빈 구성: 연결/읽기 타임아웃을 설정합니다.
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(AppSettings settings) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(settings.getHttpConnectTimeoutMs());
        f.setReadTimeout(settings.getHttpReadTimeoutMs());
        return new RestTemplate(f);
    }
}
