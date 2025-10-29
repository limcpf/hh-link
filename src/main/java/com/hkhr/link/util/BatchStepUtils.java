package com.hkhr.link.util;

import com.hkhr.link.config.AppSettings;
import com.hkhr.link.domain.Domain;
import org.springframework.batch.core.JobParameters;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

// 배치 스텝 공통 유틸(파일명/날짜 계산 등 경량 부가 로직)
public class BatchStepUtils {
    public static String resolveDate(JobParameters params) {
        String rt = params == null ? null : params.getString("requestTime");
        if (rt != null && rt.trim().length() >= 8) return rt.trim().substring(0, 8);
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    public static Path outputPathForDomain(AppSettings settings, Domain domain, String date) {
        return Paths.get(settings.getOutputDir(), domain.plural() + "-" + date + ".json");
    }

    // usersPathForDate는 더 이상 사용하지 않습니다.
}
