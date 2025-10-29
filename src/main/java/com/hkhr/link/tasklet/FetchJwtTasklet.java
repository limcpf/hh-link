package com.hkhr.link.tasklet;

import com.hkhr.link.config.AppSettings;
import com.hkhr.link.domain.Domain;
import com.hkhr.link.service.JwtService;
import com.hkhr.link.util.DebugSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

// 도메인별 JWT를 발급받아 JobExecutionContext에 저장하는 Tasklet
public class FetchJwtTasklet implements Tasklet {
    private static final Logger log = LoggerFactory.getLogger(FetchJwtTasklet.class);

    private final Domain domain;
    private final JwtService jwtService;
    private final AppSettings settings;

    public FetchJwtTasklet(Domain domain, JwtService jwtService, AppSettings settings) {
        this.domain = domain;
        this.jwtService = jwtService;
        this.settings = settings;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        DebugSupport debug = DebugSupport.from(stepExecution, settings.getOutputDir());
        String token;
        try {
            if (debug.enabled) {
                JwtService.JwtFetchResult r = jwtService.fetchTokenWithRaw(domain.key());
                token = r.token;
                // 디버그 모드: 요청/응답 및 토큰을(마스킹 적용) 파일로 덤프
                if (debug.shouldDump()) {
                    String masked = DebugSupport.maskToken(token, debug.dumpSensitive);
                    StringBuilder sb = new StringBuilder();
                    sb.append("tokenUrl=\"").append(settings.getAuthTokenUrl(domain.key())).append("\"\n");
                    String svcKey = settings.getAuthServiceKey(domain.key());
                    String svcMasked = debug.dumpSensitive ? svcKey : DebugSupport.maskToken(svcKey, false);
                    sb.append("serviceKey=").append(svcMasked).append("\n");
                    debug.write("jwt/jwt-" + domain.key() + ".txt", sb.toString());
                    debug.write("jwt/token-response-" + domain.key() + ".json", r.rawBody != null ? r.rawBody : "");
                }
            } else {
                token = jwtService.fetchToken(domain.key());
            }
        } catch (Exception e) {
            if (debug.enabled) {
                StringBuilder sb = new StringBuilder();
                sb.append("ERROR jwt fetch\n");
                sb.append("tokenUrl=").append(settings.getAuthTokenUrl(domain.key())).append('\n');
                String svcKey = settings.getAuthServiceKey(domain.key());
                String svcMasked = debug.dumpSensitive ? svcKey : DebugSupport.maskToken(svcKey, false);
                sb.append("serviceKey=").append(svcMasked).append('\n');
                sb.append("stacktrace=\n").append(DebugSupport.stackTrace(e));
                debug.write("jwt/error-jwt-" + domain.key() + ".txt", sb.toString());
            }
            throw e;
        }
        // 이후 스텝에서 사용하도록 JobExecutionContext에 저장
        stepExecution.getJobExecution().getExecutionContext().putString("jwt." + domain.key(), token);
        log.info("Stored jwt.{} in JobExecutionContext", domain.key());
        return RepeatStatus.FINISHED;
    }
}
