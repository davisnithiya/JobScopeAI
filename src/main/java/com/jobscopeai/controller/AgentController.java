package com.jobscopeai.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final String HF_TOKEN = System.getenv("HUGGINGFACE_API_TOKEN");
    private final RestTemplate rest = new RestTemplate();

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "") ;
        // Simple stub agent: echo with a note. Replace with real AI integration later.
        String result = "[Agent stub] Summary for prompt:\n" + prompt;
        return ResponseEntity.ok(Map.of("result", result));
    }
}
