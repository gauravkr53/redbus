package com.redbus.repository.impl;

import com.redbus.repository.LockRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class InMemoryLockRepository implements LockRepository {
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        return lock.tryLock();
    }

    @Override
    public void unlock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
