package com.redbus.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/healthz")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> health() {
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
