package com.jobscopeai.provider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Normalizer {

    public static Map<String, Object> normalizeAdzuna(Map<String, Object> raw) {
        String id = String.valueOf(raw.getOrDefault("id", ""));
        String title = String.valueOf(raw.getOrDefault("title", ""));
        String company = "";
        if (raw.containsKey("company") && raw.get("company") instanceof Map) {
            Map<?, ?> comp = (Map<?, ?>) raw.get("company");
            company = comp.containsKey("display_name") ? String.valueOf(comp.get("display_name")) : "";
        } else if (raw.containsKey("company")) {
            company = String.valueOf(raw.get("company"));
        }
        String location = "";
        if (raw.containsKey("location") && raw.get("location") instanceof Map) {
            Map<?, ?> loc = (Map<?, ?>) raw.get("location");
            location = loc.containsKey("display_name") ? String.valueOf(loc.get("display_name")) : "";
        } else if (raw.containsKey("location")) {
            location = String.valueOf(raw.get("location"));
        }
        String updated = raw.containsKey("updatedAt") ? String.valueOf(raw.get("updatedAt")) : (raw.containsKey("created") ? String.valueOf(raw.get("created")) : Instant.now().toString());
        String url = raw.containsKey("url") ? String.valueOf(raw.get("url")) : (raw.containsKey("redirect_url") ? String.valueOf(raw.get("redirect_url")) : "");
        List<String> skills = new ArrayList<>();
        if (raw.containsKey("description")) {
            String desc = String.valueOf(raw.get("description"));
            // naive: extract tech tokens - placeholder
            if (desc.toLowerCase().contains("java")) skills.add("Java");
            if (desc.toLowerCase().contains("react")) skills.add("React");
        }
        return Map.of(
                "id", "adzuna:" + id,
                "title", title,
                "company", company,
                "location", location,
                "skills", skills,
                "updatedAt", updated,
                "source", "Adzuna",
                "url", url
        );
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeRapid(Map<String, Object> raw) {
        // Try common fields
        String title = String.valueOf(raw.getOrDefault("title", raw.getOrDefault("job_title", "")));
        String company = String.valueOf(raw.getOrDefault("company", raw.getOrDefault("employer", "")));
        String location = String.valueOf(raw.getOrDefault("location", raw.getOrDefault("candidate_required_location", "")));
        String updated = String.valueOf(raw.getOrDefault("updatedAt", raw.getOrDefault("date", Instant.now().toString())));
        String url = String.valueOf(raw.getOrDefault("url", raw.getOrDefault("job_link", raw.getOrDefault("redirect_url", ""))));
        List<String> skills = new ArrayList<>();
        if (raw.containsKey("skills") && raw.get("skills") instanceof List) {
            for (Object s : (List<Object>) raw.get("skills")) skills.add(String.valueOf(s));
        }
        return Map.of(
                "id", "rapid:" + Math.abs(raw.hashCode()),
                "title", title,
                "company", company,
                "location", location,
                "skills", skills,
                "updatedAt", updated,
                "source", raw.getOrDefault("source", "RapidAPI"),
                "url", url
        );
    }
}
