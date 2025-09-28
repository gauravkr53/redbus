package com.redbus.repository;

public interface LockRepository {
    boolean tryLock(String key);
    void unlock(String key);
}
