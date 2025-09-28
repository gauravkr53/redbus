package com.redbus.repository.impl;

import com.redbus.model.Bus;
import com.redbus.repository.BusRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBusRepository implements BusRepository {
    private final Map<String, Bus> buses = new ConcurrentHashMap<>();

    @Override
    public Optional<Bus> findById(String busId) {
        return Optional.ofNullable(buses.get(busId));
    }

    @Override
    public List<Bus> findAll() {
        return List.copyOf(buses.values());
    }

    @Override
    public void save(Bus bus) {
        buses.put(bus.getBusId(), bus);
    }
}
