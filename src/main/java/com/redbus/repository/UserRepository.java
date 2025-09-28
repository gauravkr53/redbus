package com.redbus.repository;

import com.redbus.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String userId);
    Optional<User> findByEmail(String email);
    void save(User user);
}
