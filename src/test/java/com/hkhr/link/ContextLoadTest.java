package com.hkhr.link;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadTest {

    @Autowired(required = false)
    private Job masterJob;

    @Autowired(required = false)
    private Job userJob;

    @Autowired(required = false)
    private Job organizationJob;

    @Autowired(required = false)
    private Job attendJob;

    @Autowired(required = false)
    private Job applyJob;

    @Autowired(required = false)
    private Job accountJob;

    @Autowired(required = false)
    private Job importUsersJob;

    @Test
    void contextLoads_andJobsExist() {
        assertThat(masterJob).as("masterJob bean").isNotNull();
        assertThat(userJob).as("userJob bean").isNotNull();
        assertThat(organizationJob).as("organizationJob bean").isNotNull();
        assertThat(attendJob).as("attendJob bean").isNotNull();
        assertThat(applyJob).as("applyJob bean").isNotNull();
        assertThat(accountJob).as("accountJob bean").isNotNull();
        assertThat(importUsersJob).as("importUsersJob bean").isNotNull();
    }
}

