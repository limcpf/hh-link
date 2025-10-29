package com.hkhr.link.config;

import com.hkhr.link.domain.Domain;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class AppSettings {
    private final Environment env;

    public AppSettings(Environment env) {
        this.env = env;
    }

    public String getAuthServiceKey(String domain) { return env.getProperty("auth." + domain + ".service-key"); }
    public String getAuthTokenUrl(String domain) { return env.getProperty("auth." + domain + ".token-url"); }
    public String getListUrl(String domain) { return env.getProperty("endpoints." + domain + ".list-url"); }
    public String getByUserUrlTemplate(String domain) { return env.getProperty("endpoints." + domain + ".by-user-url-template"); }
    public String getRequestPayload(String domain) { return env.getProperty("endpoints." + domain + ".request-payload"); }
    public String getByUserPayloadTemplate(String domain) { return env.getProperty("endpoints." + domain + ".by-user-payload-template"); }

    public String getAuthServiceKey(Domain domain) { return getAuthServiceKey(domain.key()); }
    public String getAuthTokenUrl(Domain domain) { return getAuthTokenUrl(domain.key()); }
    public String getListUrl(Domain domain) { return getListUrl(domain.key()); }
    public String getByUserUrlTemplate(Domain domain) { return getByUserUrlTemplate(domain.key()); }
    public String getRequestPayload(Domain domain) { return getRequestPayload(domain.key()); }
    public String getByUserPayloadTemplate(Domain domain) { return getByUserPayloadTemplate(domain.key()); }

    public int getHttpConnectTimeoutMs() {
        return getInt("http.connect-timeout-ms", 5000);
    }

    public int getHttpReadTimeoutMs() {
        return getInt("http.read-timeout-ms", 15000);
    }

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

    public boolean isOverwrite() {
        return getBool("output.overwrite", false);
    }

    public String getOutputDir() {
        return env.getProperty("output.dir", "target/out");
    }

    public Path getUsersJsonPath() { return Paths.get(getOutputDir(), Domain.USER.plural() + ".json"); }

    public int getDbBatchSize() { return getInt("db.batch-size", 500); }

    public static boolean isIndependentDomain(String domain) {
        return Domain.fromKey(domain) != null && Domain.fromKey(domain).isIndependent();
    }

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
