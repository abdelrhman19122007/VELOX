package com.app.controller;

import com.app.dto.GovernorateResponseDto;
import com.app.enums.Governorate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for Egyptian governorates and their shipping prices.
 *
 * GET /api/governorates                    -> all governorates with prices
 * GET /api/governorates/{name}             -> single governorate (e.g. CAIRO)
 * GET /api/governorates/{name}/shipping    -> {governorate, shippingPrice, deliveryDays}
 */
@RestController
@RequestMapping("/api/governorates")
public class GovernorateController {

    @GetMapping
    public List<GovernorateResponseDto> getAll() {
        return Arrays.stream(Governorate.values())
                .map(GovernorateResponseDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{name}")
    public ResponseEntity<GovernorateResponseDto> getByName(@PathVariable String name) {
        try {
            Governorate governorate = Governorate.fromName(name);
            return ResponseEntity.ok(GovernorateResponseDto.from(governorate));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{name}/shipping")
    public ResponseEntity<Map<String, Object>> getShippingPrice(@PathVariable String name) {
        try {
            Governorate governorate = Governorate.fromName(name);
            Map<String, Object> body = Map.of(
                    "governorate", governorate.name(),
                    "arabicName", governorate.getArabicName(),
                    "shippingPrice", governorate.getShippingPrice(),
                    "deliveryDays", governorate.getDeliveryDays()
            );
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
