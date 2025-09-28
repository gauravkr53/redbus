package com.redbus.repository;

import com.redbus.model.City;
import java.util.List;
import java.util.Optional;

public interface CityRepository {
    Optional<City> findById(String cityId);
    Optional<City> findByName(String name);
    List<City> findAll();
    void save(City city);
}
