// src/main/java/com/Shubham/carDealership/controller/TradeInController.java
package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.TradeInService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trade-in")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TradeInController {

    @Autowired
    private TradeInService tradeInService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                return userRepository.findById(userId).orElse(null);
            }
        }
        return null;
    }

    private boolean isAdmin(User user) {
        return user != null && ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()));
    }

    // ============================================
    // GET ESTIMATE - Using ML Model
    // ============================================
    @PostMapping("/estimate")
    public ResponseEntity<?> estimateTradeIn(@RequestBody Map<String, Object> request) {
        String make = (String) request.get("make");
        String model = (String) request.get("model");
        Integer year = (Integer) request.get("year");
        Integer mileage = (Integer) request.get("mileage");
        String condition = (String) request.get("condition");

        // Get additional data for ML (with defaults if not provided)
        String bodyType = request.get("body_type") != null ? (String) request.get("body_type") : "Sedan";
        String fuelType = request.get("fuel_type") != null ? (String) request.get("fuel_type") : "Petrol";
        String transmission = request.get("transmission") != null ? (String) request.get("transmission") : "Automatic";
        Integer owners = request.get("owners") != null ? (Integer) request.get("owners") : 1;
        Double engineSize = request.get("engine_size") != null ? ((Number) request.get("engine_size")).doubleValue() : 2.0;

        // Use ML prediction
        BigDecimal estimate = tradeInService.calculateEstimatedValueFull(
                make, model, year, mileage, condition,
                bodyType, fuelType, transmission, owners, engineSize
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("estimatedValue", estimate);
        response.put("model", "ML_RandomForest");

        return ResponseEntity.ok(response);
    }

    // ============================================
    // SUBMIT TRADE-IN REQUEST
    // ============================================
    @PostMapping("/request")
    public ResponseEntity<?> createTradeIn(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        String rego = (String) request.get("rego");
        if (rego == null || rego.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Registration plate (rego) is required"));
        }

        // Check if rego already exists
        if (tradeInService.regoExists(rego)) {
            TradeIn existing = tradeInService.getTradeInByRego(rego);
            String status = existing.getStatus();
            String message = "";

            if ("PENDING".equals(status)) {
                message = "A trade-in request for this vehicle (Rego: " + rego + ") is already pending review.";
            } else if ("APPROVED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") has already been approved for trade-in.";
            } else if ("REJECTED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") was previously rejected.";
            } else if ("COMPLETED".equals(status)) {
                message = "This vehicle (Rego: " + rego + ") has already been traded in.";
            }

            return ResponseEntity.ok(Map.of("success", false, "message", message, "existingTradeIn", existing));
        }

        // Get trade-in details
        Long carId = request.get("carId") != null ? Long.valueOf(request.get("carId").toString()) : null;
        String make = (String) request.get("make");
        String model = (String) request.get("model");
        Integer year = (Integer) request.get("year");
        Integer mileage = (Integer) request.get("mileage");
        String condition = (String) request.get("condition");
        String notes = (String) request.get("notes");

        TradeIn tradeIn = tradeInService.createTradeIn(user.getId(), carId, rego, make, model, year, mileage, condition, notes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeIn", tradeIn);
        response.put("message", "Trade-in request submitted successfully!");

        return ResponseEntity.ok(response);
    }

    // ============================================
    // GET USER'S TRADE-INS
    // ============================================
    @GetMapping("/my-trade-ins")
    public ResponseEntity<?> getUserTradeIns(HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        List<TradeIn> tradeIns = tradeInService.getUserTradeIns(user.getId());
        return ResponseEntity.ok(Map.of("success", true, "tradeIns", tradeIns));
    }

    // ============================================
    // ADMIN: GET PENDING TRADE-INS
    // ============================================
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingTradeIns(HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        List<TradeIn> pending = tradeInService.getPendingTradeIns();
        return ResponseEntity.ok(Map.of("success", true, "tradeIns", pending));
    }

    // ============================================
    // ADMIN: APPROVE TRADE-IN
    // ============================================
    @PutMapping("/admin/{tradeInId}/approve")
    public ResponseEntity<?> approveTradeIn(@PathVariable Long tradeInId,
                                            @RequestBody Map<String, Object> request,
                                            HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        BigDecimal finalValue = BigDecimal.valueOf(Double.parseDouble(request.get("finalValue").toString()));
        TradeIn tradeIn = tradeInService.approveTradeIn(tradeInId, finalValue);

        if (tradeIn == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Trade-in not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeIn", tradeIn);
        response.put("message", "Trade-in approved successfully");

        return ResponseEntity.ok(response);
    }

    // ============================================
    // ADMIN: REJECT TRADE-IN
    // ============================================
    @PutMapping("/admin/{tradeInId}/reject")
    public ResponseEntity<?> rejectTradeIn(@PathVariable Long tradeInId,
                                           @RequestBody Map<String, Object> request,
                                           HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        String reason = (String) request.get("reason");
        TradeIn tradeIn = tradeInService.rejectTradeIn(tradeInId, reason);

        if (tradeIn == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Trade-in not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeIn", tradeIn);
        response.put("message", "Trade-in rejected");

        return ResponseEntity.ok(response);
    }

    // ============================================
    // ADMIN: GET ALL TRADE-INS (for admin view)
    // ============================================
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllTradeIns(HttpServletRequest httpRequest) {
        User admin = getAuthenticatedUser(httpRequest);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        // You can add a method in service to get all trade-ins
        // For now, get pending ones
        List<TradeIn> allTradeIns = tradeInService.getPendingTradeIns();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeIns", allTradeIns);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // CHECK IF TRADE-IN EXISTS FOR REGO
    // ============================================
    @GetMapping("/check/{rego}")
    public ResponseEntity<?> checkTradeInExists(@PathVariable String rego, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        boolean exists = tradeInService.regoExists(rego);
        TradeIn existing = exists ? tradeInService.getTradeInByRego(rego) : null;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("exists", exists);
        if (existing != null) {
            response.put("status", existing.getStatus());
            response.put("tradeInId", existing.getId());
        }

        return ResponseEntity.ok(response);
    }
}