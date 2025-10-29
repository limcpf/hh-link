package com.hkhr.link.util;

import com.hkhr.link.config.AppSettings;
import com.hkhr.link.util.enums.Domain;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BatchStepUtilsTest {

    @Test
    void resolveDate_usesRequestTime() {
        JobParameters p = new JobParametersBuilder().addString("requestTime", "20251231").toJobParameters();
        assertThat(BatchStepUtils.resolveDate(p)).isEqualTo("20251231");
    }

    @Test
    void outputPaths_buildCorrectly() {
        AppSettings settings = new AppSettings(new MockEnvironment().withProperty("output.dir", "/data/out"));
        String date = "20250103";
        Path p1 = BatchStepUtils.outputPathForDomain(settings, Domain.APPLY, date);
        assertThat(p1.toString()).endsWith("applies-20250103.json");
    }
}
