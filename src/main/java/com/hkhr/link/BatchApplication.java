package com.hkhr.link;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 애플리케이션 진입점
// - @SpringBootApplication: 스프링 부트 자동 구성
// - @EnableBatchProcessing: 스프링 배치 활성화
// - @EnableScheduling: (조건부 빈 기반) 스케줄링 사용 가능
@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
public class BatchApplication {
    public static void main(String[] args) {
        // 스프링 부트 애플리케이션을 기동합니다.
        SpringApplication.run(BatchApplication.class, args);
    }
}
