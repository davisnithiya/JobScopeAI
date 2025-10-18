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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long updatedSinceMinutes
    ) {
        Instant cutoff = updatedSinceMinutes == null ? Instant.EPOCH : Instant.now().minus(updatedSinceMinutes, ChronoUnit.MINUTES);

        List<Map<String, Object>> out = jobs.stream()
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

        return ResponseEntity.ok(out);
    }
}
