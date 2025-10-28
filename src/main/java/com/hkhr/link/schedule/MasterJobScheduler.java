package com.hkhr.link.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
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

    // 매 3분(초 0)에 실행
    @Scheduled(cron = "0 */3 * * * *")
    public void runMasterJobEvery3Minutes() throws Exception {
        String jobName = masterJob.getName();
        if (!jobExplorer.findRunningJobExecutions(jobName).isEmpty()) {
            log.info("{} is already running. Skipping this schedule.", jobName);
            return;
        }
        String requestTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        JobParameters params = new JobParametersBuilder()
                .addString("requestTime", requestTime)
                .addLong("scheduledEpochMs", System.currentTimeMillis())
                .toJobParameters();
        log.info("Scheduling {} with requestTime={}.", jobName, requestTime);
        jobLauncher.run(masterJob, params);
    }
}

