package com.hkhr.link.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateUtilsTest {

    @Test
    void buildDateVars_usesRequestTimeOrToday() {
        Map<String, String> v = TemplateUtils.buildDateVars("20250101");
        assertThat(v.get("date")).isEqualTo("20250101");
        assertThat(v.get("request_date")).isEqualTo("20250101");
        assertThat(v.get("yyyy")).isEqualTo("2025");
        assertThat(v.get("MM")).isEqualTo("01");
        assertThat(v.get("dd")).isEqualTo("01");
        assertThat(v.get("boy")).isEqualTo("20250101");
        assertThat(v.get("eoy")).isEqualTo("20251231");
        assertThat(v.get("date_last_year")).isEqualTo("20240101");
        assertThat(v.get("yyyy_last")).isEqualTo("2024");
    }

    @Test
    void apply_replacesVariables() {
        Map<String, String> v = TemplateUtils.buildDateVars("20250102");
        String t = "{\"date\":\"{date}\",\"yyyy\":\"{yyyy}\",\"eoy\":\"{eoy}\",\"last\":\"{yyyy_last}{MM}{dd}\"}";
        String out = TemplateUtils.apply(t, v);
        assertThat(out).isEqualTo("{\"date\":\"20250102\",\"yyyy\":\"2025\",\"eoy\":\"20251231\",\"last\":\"20240102\"}");
    }

    @Test
    void escapeJson_escapesQuotesAndBackslashes() {
        String s = "\\\""; // a backslash and a quote
        String e = TemplateUtils.escapeJson(s);
        assertThat(e).isEqualTo("\\\\\\\"");
    }
}

