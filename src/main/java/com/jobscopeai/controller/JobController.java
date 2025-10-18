package com.jobscopeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import com.jobscopeai.provider.AdzunaAdapter;
import com.jobscopeai.provider.RapidApiAdapter;
import com.jobscopeai.provider.Normalizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static class Job {
        public String id;
        public String title;
        public String company;
        public String location;
        public List<String> skills;
        public Instant updatedAt;
        public String source;

        public Job(String id, String title, String company, String location, List<String> skills, Instant updatedAt, String source) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.location = location;
            this.skills = skills;
            this.updatedAt = updatedAt;
            this.source = source;
        }
    }

    // Sample dataset - replace with real scrapers or API adapters later
    private final List<Job> jobs = new ArrayList<>(List.of(
            new Job("1", "Senior Java Engineer", "Acme Corp", "New York, NY", List.of("Java", "Spring", "MongoDB"), Instant.now().minus(30, ChronoUnit.MINUTES), "LinkedIn"),
            new Job("2", "Frontend Engineer (React)", "Beta LLC", "San Francisco, CA", List.of("JavaScript", "React", "TypeScript"), Instant.now().minus(2, ChronoUnit.DAYS), "Indeed"),
            new Job("3", "ML Engineer", "DataX", "Remote", List.of("Python", "TensorFlow", "PyTorch"), Instant.now().minus(6, ChronoUnit.HOURS), "Glassdoor"),
            new Job("4", "Fullstack Engineer", "Startup Y", "Austin, TX", List.of("Node.js", "React", "Postgres"), Instant.now().minus(90, ChronoUnit.MINUTES), "LinkedIn")
    ));

    // Simple in-memory caches shared across requests
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Instant> LAST_CALL = new ConcurrentHashMap<>();

    record CacheEntry(List<Map<String, Object>> data, Instant ts) {}

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
        @RequestParam(required = false) String skills,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) Long updatedSinceMinutes,
        @RequestParam(required = false) String providers
    ) {
        Instant cutoff = updatedSinceMinutes == null ? Instant.EPOCH : Instant.now().minus(updatedSinceMinutes, ChronoUnit.MINUTES);
        // Start with sample dataset
        List<Map<String, Object>> results = jobs.stream()
                .filter(j -> j.updatedAt.isAfter(cutoff))
                .filter(j -> location == null || j.location.toLowerCase(Locale.ROOT).contains(location.toLowerCase(Locale.ROOT)))
                .filter(j -> {
                    if (skills == null || skills.isBlank()) return true;
                    String[] req = skills.split(",");
                    for (String r : req) {
                        String t = r.trim().toLowerCase(Locale.ROOT);
                        boolean found = j.skills.stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(t));
                        if (!found) return false;
                    }
                    return true;
                })
                .map(j -> Map.<String, Object>of(
                        "id", j.id,
                        "title", j.title,
                        "company", j.company,
                        "location", j.location,
                        "skills", j.skills,
                        "updatedAt", j.updatedAt.toString(),
                        "source", j.source
                ))
                .collect(Collectors.toList());

        // Provider selection and configuration
        java.util.Set<String> enabled = new java.util.HashSet<>();
        if (providers == null || providers.isBlank()) {
            enabled.add("adzuna");
            enabled.add("rapidapi");
        } else {
            for (String p : providers.split(",")) enabled.add(p.trim().toLowerCase(Locale.ROOT));
        }

    final long ttlSecondsTemp = 300;
    long ttlSecondsLocal = ttlSecondsTemp;
    try { ttlSecondsLocal = Long.parseLong(System.getenv().getOrDefault("CACHE_TTL_SECONDS", "300")); } catch (Exception ignored) {}
    final long ttlSeconds = ttlSecondsLocal;
    final long minIntervalMillis = Long.parseLong(System.getenv().getOrDefault("PROVIDER_MIN_INTERVAL_MS", "1000"));

        java.util.function.BiFunction<String, Supplier<List<Map<String, Object>>>, List<Map<String, Object>>> fetchSafe = (cacheKey, supplier) -> {
            CacheEntry c = CACHE.get(cacheKey);
            Instant now = Instant.now();
            if (c != null && c.ts.plusSeconds(ttlSeconds).isAfter(now)) return c.data;
            Instant last = LAST_CALL.get(cacheKey);
            if (last != null && last.plusMillis(minIntervalMillis).isAfter(now)) return List.of();
            LAST_CALL.put(cacheKey, now);
            List<Map<String, Object>> res = supplier.get();
            if (res != null) CACHE.put(cacheKey, new CacheEntry(res, now));
            return res == null ? List.of() : res;
        };

        // If Adzuna credentials are present and enabled, fetch live results and merge (normalized)
        String adzId = System.getenv("ADZUNA_APP_ID");
        String adzKey = System.getenv("ADZUNA_APP_KEY");
        if (enabled.contains("adzuna") && adzId != null && !adzId.isBlank() && adzKey != null && !adzKey.isBlank()) {
            AdzunaAdapter adz = new AdzunaAdapter(adzId, adzKey, System.getenv("ADZUNA_COUNTRY"));
            List<Map<String, Object>> adzResults = fetchSafe.apply("adzuna:" + skills + ":" + location, () -> adz.fetch(skills, location, 20));
            results.addAll(adzResults.stream()
                    .map(m -> Normalizer.normalizeAdzuna(m))
                    .filter(m -> {
                        String updated = String.valueOf(m.getOrDefault("updatedAt", Instant.now().toString()));
                        try { return Instant.parse(updated).isAfter(cutoff); } catch (Exception ex) { return true; }
                    })
                    .collect(Collectors.toList())
            );
        }

        // RapidAPI integration (if enabled)
        String rapidKey = System.getenv("RAPIDAPI_KEY");
        String rapidProviders = System.getenv("RAPIDAPI_PROVIDERS");
        if (enabled.contains("rapidapi") && rapidKey != null && !rapidKey.isBlank() && rapidProviders != null && !rapidProviders.isBlank()) {
            RapidApiAdapter rapid = new RapidApiAdapter(rapidKey);
            String[] providersArr = rapidProviders.split(",");
            for (String p : providersArr) {
                String[] parts = p.split("\\|", 2);
                if (parts.length != 2) continue;
                String host = parts[0].trim();
                String path = parts[1].trim();
                String cacheKey = "rapid:" + host + ":" + path + ":" + skills + ":" + location;
                List<Map<String, Object>> r = fetchSafe.apply(cacheKey, () -> rapid.fetch(host, path));
                results.addAll(r.stream().map(m -> Normalizer.normalizeRapid(m)).collect(Collectors.toList()));
            }
        }

        // Deduplicate by title+company+location (case-insensitive)
        List<Map<String, Object>> dedup = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map<String, Object> r : results) {
            String key = (String.valueOf(r.getOrDefault("title", "")) + "|" + String.valueOf(r.getOrDefault("company", "")) + "|" + String.valueOf(r.getOrDefault("location", ""))).toLowerCase(Locale.ROOT);
            if (seen.contains(key)) continue;
            seen.add(key);
            dedup.add(r);
        }

        return ResponseEntity.ok(dedup);
    }
}
