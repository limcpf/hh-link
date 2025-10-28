package com.hkhr.link.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MasterJobSchedulerTest {

    @Test
    void skipsWhenAlreadyRunning() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        when(job.getName()).thenReturn("masterJob");
        JobExplorer explorer = mock(JobExplorer.class);
        Set<JobExecution> running = new HashSet<>();
        running.add(new JobExecution(1L));
        when(explorer.findRunningJobExecutions("masterJob")).thenReturn(running);

        MasterJobScheduler scheduler = new MasterJobScheduler(launcher, job, explorer);

        scheduler.runMasterJobEvery3Minutes();

        verify(launcher, never()).run(any(), any());
    }

    @Test
    void triggersRunWithParameters() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        when(launcher.run(any(), any())).thenReturn(new JobExecution(2L));
        Job job = mock(Job.class);
        when(job.getName()).thenReturn("masterJob");
        JobExplorer explorer = mock(JobExplorer.class);
        when(explorer.findRunningJobExecutions("masterJob")).thenReturn(Collections.emptySet());

        MasterJobScheduler scheduler = new MasterJobScheduler(launcher, job, explorer);

        scheduler.runMasterJobEvery3Minutes();

        ArgumentCaptor<JobParameters> params = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(1)).run(eq(job), params.capture());
        JobParameters p = params.getValue();
        assertThat(p.getString("requestTime")).isNotBlank();
        assertThat(p.getLong("scheduledEpochMs")).isNotNull();
    }
}

