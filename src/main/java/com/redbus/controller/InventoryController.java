package com.redbus.controller;

import com.redbus.dto.SearchResponseItem;
import com.redbus.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/search")
    public ResponseEntity<List<SearchResponseItem>> searchTrips(
            @RequestParam String date,
            @RequestParam String sourceCityId,
            @RequestParam String destCityId) {

        List<SearchResponseItem> response = inventoryService.search(sourceCityId, destCityId, date);
        return ResponseEntity.ok(response);
    }  
}
