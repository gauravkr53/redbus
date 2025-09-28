package com.redbus.controller;

import com.redbus.dto.CreateTripRequest;
import com.redbus.dto.TripView;
import com.redbus.model.Trip;
import com.redbus.model.PricingType;
import com.redbus.repository.BusRepository;
import com.redbus.repository.CityRepository;
import com.redbus.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    private final CityRepository cityRepository;
    private final BusRepository busRepository;
    
    @GetMapping("")
    public String adminIndex() {
        return "admin/index";
    }
    
    @GetMapping("/add-trip")
    public String addTripPage(Model model) {
        model.addAttribute("cities", cityRepository.findAll());
        model.addAttribute("buses", busRepository.findAll());
        model.addAttribute("pricingTypes", PricingType.values());
        return "admin/add-trip";
    }
    
    @PostMapping("/create-trip")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        try {
            List<Trip> createdTrips = adminService.createTrip(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully created " + createdTrips.size() + " trip(s)");
            response.put("tripIds", createdTrips.stream().map(Trip::getTripId).toList());
            
            log.info("Created {} trips for request: {}", createdTrips.size(), request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error creating trip: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create trip: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/trips")
    public String tripsPage(Model model) {
        List<TripView> trips = adminService.getAllTripsForDisplay();
        model.addAttribute("trips", trips);
        log.info("Loaded {} trips for admin view", trips.size());
        return "admin/trips";
    }
}
