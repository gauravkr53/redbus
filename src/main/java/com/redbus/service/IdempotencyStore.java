package com.redbus.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyStore {
    private final Map<String, StoredResult> store = new ConcurrentHashMap<>();

    public void storeResult(String key, Object result, LocalDateTime expiresAt) {
        store.put(key, new StoredResult(result, expiresAt));
    }

    public Object getResult(String key) {
        StoredResult stored = store.get(key);
        if (stored == null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            store.remove(key);
            return null;
        }
        return stored.getResult();
    }

    public void removeExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoredResult {
        private Object result;
        private LocalDateTime expiresAt;
    }
}
