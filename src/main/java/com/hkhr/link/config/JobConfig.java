package com.hkhr.link.config;

import com.hkhr.link.util.enums.Domain;
import com.hkhr.link.service.JwtService;
import com.hkhr.link.tasklet.FetchAndSaveJsonTasklet;
import com.hkhr.link.tasklet.FetchJwtTasklet;
import com.hkhr.link.tasklet.InsertUsersFromJsonTasklet;
import com.hkhr.link.db.UserMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.JobStepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// 배치 잡/스텝 구성
@Configuration
public class JobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final AppSettings settings;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    public JobConfig(JobBuilderFactory jobBuilderFactory,
                     StepBuilderFactory stepBuilderFactory,
                     AppSettings settings, RestTemplate restTemplate, JwtService jwtService) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.settings = settings;
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
    }

    // FetchJWT 스텝(도메인별 토큰 발급)
    private Step fetchJwt(Domain domain) {
        return stepBuilderFactory
                .get(domain.stepName("fetchJwt"))
                .tasklet(new FetchJwtTasklet(domain, jwtService, settings))
                .build();
    }

    // FetchAndSave 스텝(도메인별 데이터 수집/파일 저장)
    private Step fetchAndSave(Domain domain) {
        return stepBuilderFactory
                .get(domain.stepName("fetchAndSave"))
                .tasklet(new FetchAndSaveJsonTasklet(domain, settings, restTemplate))
                .build();
    }

    // 도메인 Job: FetchJWT → FetchAndSave 순서의 2스텝
    private Job domainJob(Domain domain) {
        JobBuilder jb = jobBuilderFactory.get(domain.jobName());
        return jb.incrementer(new RunIdIncrementer())
                .start(fetchJwt(domain))
                .next(fetchAndSave(domain))
                .build();
    }

    @Bean
    public Job userJob() { return domainJob(Domain.USER); }

    @Bean
    public Job organizationJob() { return domainJob(Domain.ORGANIZATION); }

    @Bean
    public Job attendJob() { return domainJob(Domain.ATTEND); }

    @Bean
    public Job applyJob() { return domainJob(Domain.APPLY); }

    @Bean
    public Job accountJob() { return domainJob(Domain.ACCOUNT); }

    @Bean
    public Job masterJob(JobLauncher jobLauncher, JobRepository jobRepository) {
        Step userJobStep = new JobStepBuilder(stepBuilderFactory.get("userJobStep"))
                .job(userJob())
                .launcher(jobLauncher)
                .repository(jobRepository)
                .build();
        Step organizationJobStep = new JobStepBuilder(stepBuilderFactory.get("organizationJobStep"))
                .job(organizationJob())
                .launcher(jobLauncher)
                .repository(jobRepository)
                .build();
        Step attendJobStep = new JobStepBuilder(stepBuilderFactory.get("attendJobStep"))
                .job(attendJob())
                .launcher(jobLauncher)
                .repository(jobRepository)
                .build();
        Step applyJobStep = new JobStepBuilder(stepBuilderFactory.get("applyJobStep"))
                .job(applyJob())
                .launcher(jobLauncher)
                .repository(jobRepository)
                .build();
        Step accountJobStep = new JobStepBuilder(stepBuilderFactory.get("accountJobStep"))
                .job(accountJob())
                .launcher(jobLauncher)
                .repository(jobRepository)
                .build();

        // 마스터 Job: user → organization → attend → apply → account 순차 실행
        return jobBuilderFactory.get("masterJob")
                .incrementer(new RunIdIncrementer())
                .start(userJobStep)
                .next(organizationJobStep)
                .next(attendJobStep)
                .next(applyJobStep)
                .next(accountJobStep)
                .build();
    }

    // 예시: JSON 사용자 목록을 Oracle USERS 테이블에 적재(MyBatis)
    private Step insertUsersStep(UserMapper userMapper) {
        return stepBuilderFactory.get("insertUsersStep")
                .tasklet(new InsertUsersFromJsonTasklet(settings, userMapper))
                .build();
    }

    @Bean
    public Job importUsersJob(UserMapper userMapper) {
        return jobBuilderFactory.get("importUsersJob")
                .incrementer(new RunIdIncrementer())
                .start(insertUsersStep(userMapper))
                .build();
    }
}
