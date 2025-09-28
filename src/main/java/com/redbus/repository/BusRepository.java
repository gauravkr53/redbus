package com.redbus.repository;

import com.redbus.model.Bus;
import java.util.List;
import java.util.Optional;

public interface BusRepository {
    Optional<Bus> findById(String busId);
    List<Bus> findAll();
    void save(Bus bus);
}
