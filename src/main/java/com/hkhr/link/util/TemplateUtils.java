package com.hkhr.link.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// 간단한 템플릿 유틸리티: {변수} 치환
public class TemplateUtils {
    // JSON 문자열 값으로 안전하게 치환하기 위한 간단한 이스케이프
    public static String escapeJson(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    public static Map<String, String> buildDateVars(String requestTime) {
        Map<String, String> vars = new HashMap<String, String>();
        Date now = new Date();
        Date base = now;
        if (requestTime != null && requestTime.trim().length() >= 8) {
            try { base = new SimpleDateFormat("yyyyMMdd").parse(requestTime.trim().substring(0, 8)); }
            catch (ParseException ignored) { base = now; }
        }
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dfDateTime = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat dfYYYY = new SimpleDateFormat("yyyy");
        SimpleDateFormat dfMM = new SimpleDateFormat("MM");
        SimpleDateFormat dfDD = new SimpleDateFormat("dd");
        String dateStr = dfDate.format(base);
        vars.put("request_date", dateStr); // 하위 호환
        vars.put("date", dateStr);         // 권장 변수명
        // 시간은 요구사항에 따라 제거(정해진 시각이 없다면 000000으로 고정)
        vars.put("request_datetime", dateStr + "000000");
        vars.put("yyyy", dfYYYY.format(base));
        vars.put("MM", dfMM.format(base));
        vars.put("dd", dfDD.format(base));

        // 추가 파생 변수들
        Calendar cal = Calendar.getInstance();
        cal.setTime(base);

        // 연초일 yyyy0101
        Calendar boy = (Calendar) cal.clone();
        boy.set(Calendar.MONTH, Calendar.JANUARY);
        boy.set(Calendar.DAY_OF_MONTH, 1);
        vars.put("boy", dfDate.format(boy.getTime()));
        vars.put("year_start", dfDate.format(boy.getTime()));
        vars.put("start_of_year", dfDate.format(boy.getTime()));

        // 연말일 yyyy1231
        Calendar eoy = (Calendar) cal.clone();
        eoy.set(Calendar.MONTH, Calendar.DECEMBER);
        eoy.set(Calendar.DAY_OF_MONTH, 31);
        vars.put("eoy", dfDate.format(eoy.getTime()));
        vars.put("year_end", dfDate.format(eoy.getTime()));
        vars.put("end_of_year", dfDate.format(eoy.getTime()));

        // 1년 전 같은 월/일
        Calendar lastYear = (Calendar) cal.clone();
        lastYear.add(Calendar.YEAR, -1);
        vars.put("date_last_year", dfDate.format(lastYear.getTime()));
        vars.put("date_prev_year", dfDate.format(lastYear.getTime()));
        // 조합용: 작년 연도(yyyy-1)
        vars.put("yyyy_last", new SimpleDateFormat("yyyy").format(lastYear.getTime()));
        vars.put("year_prev", new SimpleDateFormat("yyyy").format(lastYear.getTime()));
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
