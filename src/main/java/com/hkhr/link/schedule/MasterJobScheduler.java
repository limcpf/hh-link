package com.hkhr.link.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

// 스케줄러 컴포넌트: 프로퍼티 scheduler.enabled=true 일 때만 등록됩니다.
@Component
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true")
public class MasterJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(MasterJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job masterJob;
    private final JobExplorer jobExplorer;

    public MasterJobScheduler(JobLauncher jobLauncher,
                              @Qualifier("masterJob") Job masterJob,
                              JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.masterJob = masterJob;
        this.jobExplorer = jobExplorer;
    }

    // 매 3분(초 0)에 실행되는 크론 스케줄
    @Scheduled(cron = "0 */3 * * * *")
    public void runMasterJobEvery3Minutes() throws Exception {
        String jobName = masterJob.getName();
        if (!jobExplorer.findRunningJobExecutions(jobName).isEmpty()) {
            // 중복 실행 방지: 이미 실행 중이면 스킵
            log.info("{} is already running. Skipping this schedule.", jobName);
            return;
        }
        // 요청 시간은 yyyyMMdd 형식만 사용합니다.
        String requestTime = new SimpleDateFormat("yyyyMMdd").format(new Date());
        JobParameters params = new JobParametersBuilder()
                .addString("requestTime", requestTime)
                .addLong("scheduledEpochMs", System.currentTimeMillis())
                .toJobParameters();
        log.info("Scheduling {} with requestTime={}.", jobName, requestTime);
        jobLauncher.run(masterJob, params);
    }
}
