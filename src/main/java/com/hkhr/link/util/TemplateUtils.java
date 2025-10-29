package com.hkhr.link.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// 간단한 템플릿 유틸리티: {변수} 치환
public class TemplateUtils {
    public static Map<String, String> buildDateVars(String requestTime) {
        Map<String, String> vars = new HashMap<String, String>();
        Date now = new Date();
        Date base = now;
        if (requestTime != null && requestTime.trim().length() >= 8) {
            try {
                // yyyyMMddHHmmss 또는 yyyyMMdd 형식 지원
                if (requestTime.trim().length() >= 14) {
                    base = new SimpleDateFormat("yyyyMMddHHmmss").parse(requestTime.trim());
                } else {
                    base = new SimpleDateFormat("yyyyMMdd").parse(requestTime.trim().substring(0, 8));
                }
            } catch (ParseException ignored) {
                base = now;
            }
        }
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dfDateTime = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dfYYYY = new SimpleDateFormat("yyyy");
        SimpleDateFormat dfMM = new SimpleDateFormat("MM");
        SimpleDateFormat dfDD = new SimpleDateFormat("dd");
        vars.put("request_date", dfDate.format(base));
        vars.put("request_datetime", dfDateTime.format(base));
        vars.put("yyyy", dfYYYY.format(base));
        vars.put("MM", dfMM.format(base));
        vars.put("dd", dfDD.format(base));
        return vars;
    }

    public static String apply(String template, Map<String, String> vars) {
        if (template == null) return null;
        String out = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }
}

