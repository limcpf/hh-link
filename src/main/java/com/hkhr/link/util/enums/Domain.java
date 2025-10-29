package com.hkhr.link.util.enums;

// 공용 Enum: 도메인 식별 및 이름/파일명/잡/스텝명 생성에 사용
public enum Domain {
    USER("user"),
    ORGANIZATION("organization"),
    ATTEND("attend"),
    APPLY("apply"),
    ACCOUNT("account");

    private final String key;

    Domain(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    // 복수형(파일명 등에 사용): apply → applies, 나머지는 접미사 s
    public String plural() {
        if (this == APPLY) return "applies";
        return key + "s";
    }

    // 잡/스텝 이름 규칙
    public String jobName() { return key + "Job"; }
    public String stepName(String prefix) { return prefix + "-" + key; }

    // 소문자 키로부터 역변환
    public static Domain fromKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        for (Domain d : values()) if (d.key.equals(k)) return d;
        return null;
    }
}

