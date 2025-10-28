package com.hkhr.link.config;

import com.hkhr.link.domain.Domain;
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

    private Step fetchJwt(Domain domain) {
        return stepBuilderFactory
                .get(domain.stepName("fetchJwt"))
                .tasklet(new FetchJwtTasklet(domain, jwtService, settings))
                .build();
    }

    private Step fetchAndSave(Domain domain) {
        return stepBuilderFactory
                .get(domain.stepName("fetchAndSave"))
                .tasklet(new FetchAndSaveJsonTasklet(domain, settings, restTemplate))
                .build();
    }

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

        return jobBuilderFactory.get("masterJob")
                .incrementer(new RunIdIncrementer())
                .start(userJobStep)
                .next(organizationJobStep)
                .next(attendJobStep)
                .next(applyJobStep)
                .next(accountJobStep)
                .build();
    }

    // Example: Import users from JSON to Oracle via MyBatis
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
