package com.redbus.config;

import com.redbus.model.*;
import com.redbus.repository.*;
import com.redbus.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final CityRepository cityRepository;
    private final BusRepository busRepository;
    private final InventoryRepository inventoryRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) throws Exception {
        seedCities();
        seedBuses();
        seedTrips();
        seedUsers();
        log.info("Data seeding completed");
    }

    private void seedCities() {
        List<City> cities = Arrays.asList(
            City.builder().cityId("BLR").name("Bangalore").latitude(12.9716).longitude(77.5946).build(),
            City.builder().cityId("MUM").name("Mumbai").latitude(19.0760).longitude(72.8777).build(),
            City.builder().cityId("DEL").name("Delhi").latitude(28.7041).longitude(77.1025).build(),
            City.builder().cityId("CHN").name("Chennai").latitude(13.0827).longitude(80.2707).build(),
            City.builder().cityId("KOL").name("Kolkata").latitude(22.5726).longitude(88.3639).build(),
            City.builder().cityId("HYD").name("Hyderabad").latitude(17.3850).longitude(78.4867).build(),
            City.builder().cityId("PUN").name("Pune").latitude(18.5204).longitude(73.8567).build(),
            City.builder().cityId("AHM").name("Ahmedabad").latitude(23.0225).longitude(72.5714).build(),
            City.builder().cityId("JAIPUR").name("Jaipur").latitude(26.9124).longitude(75.7873).build(),
            City.builder().cityId("LKO").name("Lucknow").latitude(26.8467).longitude(80.9462).build(),
            City.builder().cityId("KANPUR").name("Kanpur").latitude(26.4499).longitude(80.3319).build(),
            City.builder().cityId("NAGPUR").name("Nagpur").latitude(21.1458).longitude(79.0882).build(),
            City.builder().cityId("INDORE").name("Indore").latitude(22.7196).longitude(75.8577).build(),
            City.builder().cityId("BHOPAL").name("Bhopal").latitude(23.2599).longitude(77.4126).build(),
            City.builder().cityId("VISAKHAPATNAM").name("Visakhapatnam").latitude(17.6868).longitude(83.2185).build()
        );

        cities.forEach(cityRepository::save);
        log.info("Seeded {} cities", cities.size());
    }

    private void seedBuses() {
        List<Bus> buses = Arrays.asList(
            Bus.builder().busId("BUS001").ownerId("OWNER001").operator("RedBus Express").parkingAddress("Bangalore Depot").build(),
            Bus.builder().busId("BUS002").ownerId("OWNER002").operator("KSRTC").parkingAddress("Mysore Depot").build(),
            Bus.builder().busId("BUS003").ownerId("OWNER003").operator("BMTC").parkingAddress("Bangalore Central").build(),
            Bus.builder().busId("BUS004").ownerId("OWNER004").operator("Volvo Travels").parkingAddress("Mumbai Depot").build(),
            Bus.builder().busId("BUS005").ownerId("OWNER005").operator("Raj Travels").parkingAddress("Delhi Depot").build(),
            Bus.builder().busId("BUS006").ownerId("OWNER006").operator("Orange Travels").parkingAddress("Chennai Depot").build(),
            Bus.builder().busId("BUS007").ownerId("OWNER007").operator("SRS Travels").parkingAddress("Hyderabad Depot").build(),
            Bus.builder().busId("BUS008").ownerId("OWNER008").operator("Neeta Travels").parkingAddress("Pune Depot").build(),
            Bus.builder().busId("BUS009").ownerId("OWNER009").operator("Sharma Travels").parkingAddress("Kolkata Depot").build(),
            Bus.builder().busId("BUS010").ownerId("OWNER010").operator("Patel Travels").parkingAddress("Ahmedabad Depot").build()
        );

        buses.forEach(busRepository::save);
        log.info("Seeded {} buses", buses.size());
    }

    private void seedTrips() {
        List<String> cityIds = Arrays.asList("BLR", "MUM", "DEL", "CHN", "KOL", "HYD", "PUN", "AHM", "JAIPUR", "LKO");
        List<String> busIds = Arrays.asList("BUS001", "BUS002", "BUS003", "BUS004", "BUS005", "BUS006", "BUS007", "BUS008", "BUS009", "BUS010");
        List<PricingType> pricingTypes = Arrays.asList(PricingType.values());

        LocalDate today = LocalDate.now();
        int tripCount = 0;

        

        // Create trips for next 30 days
        for (int day = 1; day < 2; day++) {
            LocalDate date = today.plusDays(day);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Create 2-3 trips per day
            int tripsPerDay = 2 + (day % 2);
            for (int i = 0; i < tripsPerDay; i++) {
                String sourceCity = cityIds.get((day + i) % cityIds.size());
                String intermediateCity = cityIds.get((day + i + 1) % cityIds.size());
                String destCity = cityIds.get((day + i + 2) % cityIds.size());
                String busId = busIds.get((day + i) % busIds.size());
                PricingType pricingType = pricingTypes.get((day + i) % pricingTypes.size());

                Trip trip = Trip.builder()
                        .tripId("TRIP" + String.format("%03d", ++tripCount))
                        .busId(busId)
                        .date(dateStr)
                        .sourceCityId(sourceCity)
                        .destCityId(destCity)
                        .capacity(40 + (i * 10)) // 40-60 seats
                        .pricingType(pricingType)
                        .build();

                inventoryRepository.upsertTrip(trip);

                // Create trip parts
                List<TripPart> parts = Arrays.asList(
                    TripPart.builder()
                            .tripPartId("SCHED" + tripCount + "_1")
                            .tripId(trip.getTripId())
                            .date(dateStr)
                            .sourceCityId(sourceCity)
                            .destCityId(intermediateCity)
                            .sourceTime("08:00")
                            .destTime("14:00")
                            .sequence(1)
                            .capacity(trip.getCapacity())
                            .availableSeats(trip.getCapacity())
                            .build(),
                    TripPart.builder()
                            .tripPartId("SCHED" + tripCount + "_2")
                            .tripId(trip.getTripId())
                            .date(dateStr)
                            .sourceCityId(intermediateCity)
                            .destCityId(destCity)
                            .sourceTime("18:00")
                            .destTime("23:00")
                            .sequence(2)
                            .capacity(trip.getCapacity())
                            .availableSeats(trip.getCapacity())
                            .build()
                );

                inventoryRepository.upsertParts(parts);
            }
        }

        log.info("Seeded {} trips across 30 days", tripCount);
    }

    private void seedUsers() {
        authService.signup("user1@example.com", "password1");
        authService.signup("user2@example.com", "password2");
    }
}
