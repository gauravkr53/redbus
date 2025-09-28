package com.redbus.repository.impl;

import com.redbus.model.City;
import com.redbus.repository.CityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCityRepository implements CityRepository {
    private final Map<String, City> cities = new ConcurrentHashMap<>();
    private final Map<String, City> citiesByName = new ConcurrentHashMap<>();

    @Override
    public Optional<City> findById(String cityId) {
        return Optional.ofNullable(cities.get(cityId));
    }

    @Override
    public Optional<City> findByName(String name) {
        return Optional.ofNullable(citiesByName.get(name.toLowerCase()));
    }

    @Override
    public List<City> findAll() {
        return List.copyOf(cities.values());
    }

    @Override
    public void save(City city) {
        cities.put(city.getCityId(), city);
        citiesByName.put(city.getName().toLowerCase(), city);
    }
}
