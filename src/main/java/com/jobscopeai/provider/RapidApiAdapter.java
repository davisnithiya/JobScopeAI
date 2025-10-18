package com.jobscopeai.provider;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic RapidAPI adapter: expects caller to provide host and path for specific provider.
 * Configure providers via environment variables; adapter will pass x-rapidapi-host and x-rapidapi-key headers.
 */
public class RapidApiAdapter {
    private final String rapidKey;
    private final RestTemplate rest = new RestTemplate();

    public RapidApiAdapter(String rapidKey) {
        this.rapidKey = rapidKey;
    }

    /**
     * Fetch results from RapidAPI provider.
     * host: e.g. 'example-rapidapi-host.p.rapidapi.com'
     * path: e.g. '/jobs/search'
     * params: query string parameters already encoded
     */
    public List<Map<String, Object>> fetch(String host, String pathWithQuery) {
        try {
            String url = "https://" + host + pathWithQuery;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-rapidapi-key", rapidKey);
            headers.set("x-rapidapi-host", host);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            Object resp = rest.getForObject(url, Object.class);
            if (resp == null) return List.of();

            // Normalize: if resp is a Map with 'results' or 'data' array, extract it; if it's an array, use it directly
            if (resp instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) resp;
                if (m.containsKey("results") && m.get("results") instanceof List) {
                    return castList((List<?>) m.get("results"));
                }
                if (m.containsKey("data") && m.get("data") instanceof List) {
                    return castList((List<?>) m.get("data"));
                }
                // fallback: wrap map as single item
                return List.of(Map.of("raw", m));
            }
            if (resp instanceof List) {
                return castList((List<?>) resp);
            }

            return List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(List<?> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map) out.add((Map<String, Object>) o);
        }
        return out;
    }
}
