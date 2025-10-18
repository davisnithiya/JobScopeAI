package com.jobscopeai.provider;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdzunaAdapter {
    private final String appId;
    private final String appKey;
    private final String country;
    private final RestTemplate rest = new RestTemplate();

    public AdzunaAdapter(String appId, String appKey, String country) {
        this.appId = appId;
        this.appKey = appKey;
        this.country = country == null || country.isBlank() ? "us" : country;
    }

    public List<Map<String, Object>> fetch(String what, String where, int results) {
        try {
            String url = String.format("https://api.adzuna.com/v1/api/jobs/%s/search/1?app_id=%s&app_key=%s&results_per_page=%d", country, appId, appKey, results);
            if (what != null && !what.isBlank()) url += "&what=" + java.net.URLEncoder.encode(what, java.nio.charset.StandardCharsets.UTF_8);
            if (where != null && !where.isBlank()) url += "&where=" + java.net.URLEncoder.encode(where, java.nio.charset.StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            Map<?, ?> resp = rest.postForObject(url, entity, Map.class);
            if (resp == null || !resp.containsKey("results")) return List.of();

            List<?> resultsArr = (List<?>) resp.get("results");
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : resultsArr) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> r = (Map<?, ?>) o;
                String id = r.containsKey("id") ? String.valueOf(r.get("id")) : "";
                String title = r.containsKey("title") ? String.valueOf(r.get("title")) : "";
                String company = "";
                if (r.containsKey("company") && r.get("company") instanceof Map) {
                    Map<?, ?> comp = (Map<?, ?>) r.get("company");
                    company = comp.containsKey("display_name") ? String.valueOf(comp.get("display_name")) : "";
                }
                String location = "";
                if (r.containsKey("location") && r.get("location") instanceof Map) {
                    Map<?, ?> loc = (Map<?, ?>) r.get("location");
                    location = loc.containsKey("display_name") ? String.valueOf(loc.get("display_name")) : "";
                }
                String description = r.containsKey("description") ? String.valueOf(r.get("description")) : "";
                String created = r.containsKey("created") ? String.valueOf(r.get("created")) : Instant.now().toString();
                String redirectUrl = r.containsKey("redirect_url") ? String.valueOf(r.get("redirect_url")) : "";
                Object salaryMin = r.containsKey("salary_min") ? r.get("salary_min") : null;
                Object salaryMax = r.containsKey("salary_max") ? r.get("salary_max") : null;

                out.add(Map.of(
                        "id", "adzuna:" + id,
                        "title", title,
                        "company", company,
                        "location", location,
                        "description", description,
                        "updatedAt", created,
                        "source", "Adzuna",
                        "url", redirectUrl,
                        "salaryMin", salaryMin == null ? null : salaryMin.toString(),
                        "salaryMax", salaryMax == null ? null : salaryMax.toString()
                ));
            }

            return out;
        } catch (Exception ex) {
            // don't throw - return empty and let caller fall back
            return List.of();
        }
    }
}
