package com.hkhr.link.config;

import com.hkhr.link.domain.Domain;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

// 환경변수/프로퍼티에서 배치 설정 값을 읽어오는 헬퍼 컴포넌트
@Component
public class AppSettings {
    private final Environment env;

    public AppSettings(Environment env) {
        this.env = env;
    }

    // 인증/엔드포인트(도메인 문자열 기반)
    public String getAuthServiceKey(String domain) { return env.getProperty("auth." + domain + ".service-key"); }
    public String getAuthTokenUrl(String domain) { return env.getProperty("auth." + domain + ".token-url"); }
    public String getGlobalAuthTokenUrl() { return env.getProperty("auth.token-url"); }
    public String getListUrl(String domain) { return env.getProperty("endpoints." + domain + ".list-url"); }
    public String getByUserUrlTemplate(String domain) { return env.getProperty("endpoints." + domain + ".by-user-url-template"); }
    public String getApiUrl(String domain) { return env.getProperty("endpoints." + domain + ".url"); }
    public String getRequestPayload(String domain) { return env.getProperty("endpoints." + domain + ".request-payload"); }
    public String getByUserPayloadTemplate(String domain) { return env.getProperty("endpoints." + domain + ".by-user-payload-template"); }

    // 인증/엔드포인트(도메인 Enum 기반 오버로드)
    public String getAuthServiceKey(Domain domain) { return getAuthServiceKey(domain.key()); }
    public String getAuthTokenUrl(Domain domain) { return getAuthTokenUrl(domain.key()); }
    public String getListUrl(Domain domain) { return getListUrl(domain.key()); }
    public String getByUserUrlTemplate(Domain domain) { return getByUserUrlTemplate(domain.key()); }
    public String getApiUrl(Domain domain) { return getApiUrl(domain.key()); }
    public String getRequestPayload(Domain domain) { return getRequestPayload(domain.key()); }
    public String getByUserPayloadTemplate(Domain domain) { return getByUserPayloadTemplate(domain.key()); }

    // HTTP 연결/읽기 타임아웃(ms)
    public int getHttpConnectTimeoutMs() { return getInt("http.connect-timeout-ms", 5000); }

    public int getHttpReadTimeoutMs() { return getInt("http.read-timeout-ms", 15000); }

    // 사용자 단위 병렬 처리 스레드 수(1~6 사이로 캡핑)
    public int getMaxThreads() {
        int v = getInt("fetch.max-threads", 6);
        if (v < 1) v = 1;
        if (v > 6) v = 6; // cap to 6
        return v;
    }

    public boolean isContinueOnError() {
        return getBool("fetch.continue-on-error", false);
    }

    public boolean isPretty() {
        return getBool("output.pretty", false);
    }

    public boolean isOverwrite() { return getBool("output.overwrite", true); }

    public String getOutputDir() { return env.getProperty("output.dir", "target/out"); }

    // 종속 도메인 참조용 users-YYYYMMDD.json 경로(오늘 날짜 기준)
    public Path getUsersJsonPath() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return Paths.get(getOutputDir(), Domain.USER.plural() + "-" + date + ".json");
    }

    public int getDbBatchSize() { return getInt("db.batch-size", 500); }

    // 문자열 기준 독립 도메인 여부(user/organization)
    public static boolean isIndependentDomain(String domain) {
        return Domain.fromKey(domain) != null && Domain.fromKey(domain).isIndependent();
    }

    // 문자열 기준 복수형 변환(예: apply -> applies)
    public static String pluralize(String domain) {
        Domain d = Domain.fromKey(domain);
        if (d != null) return d.plural();
        // fallback
        return domain.endsWith("s") ? domain : domain + "s";
    }

    private int getInt(String key, int def) {
        String v = env.getProperty(key);
        if (v == null || v.trim().isEmpty()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private boolean getBool(String key, boolean def) {
        String v = env.getProperty(key);
        if (v == null || v.trim().isEmpty()) return def;
        return Boolean.parseBoolean(v.trim());
    }
}
