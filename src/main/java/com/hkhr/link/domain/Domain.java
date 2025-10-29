package com.hkhr.link.domain;

// 도메인 식별자(잡/스텝/설정 키 생성에 사용)
public enum Domain {
    USER("user", true),
    ORGANIZATION("organization", true),
    ATTEND("attend", false),
    APPLY("apply", false),
    ACCOUNT("account", false);

    private final String key;
    private final boolean independent;

    Domain(String key, boolean independent) {
        this.key = key;
        this.independent = independent;
    }

    public String key() {
        return key;
    }

    public boolean isIndependent() {
        return independent;
    }

    // 복수형(파일명 등에 사용): apply → applies, 나머지 접미사 s
    public String plural() {
        if (this == APPLY) return "applies";
        return key + "s";
    }

    // 잡 이름 규칙: <key>Job
    public String jobName() {
        return key + "Job";
    }

    // 스텝 이름 규칙: <prefix>-<key>
    public String stepName(String prefix) {
        return prefix + "-" + key;
    }

    // 소문자 키 문자열로부터 Enum을 역변환
    public static Domain fromKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        for (Domain d : values()) {
            if (d.key.equals(k)) return d;
        }
        return null;
    }
}
