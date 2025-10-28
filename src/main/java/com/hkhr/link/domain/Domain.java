package com.hkhr.link.domain;

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

    public String plural() {
        if (this == APPLY) return "applies";
        return key + "s";
    }

    public String jobName() {
        return key + "Job";
    }

    public String stepName(String prefix) {
        return prefix + "-" + key;
    }

    public static Domain fromKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        for (Domain d : values()) {
            if (d.key.equals(k)) return d;
        }
        return null;
    }
}

