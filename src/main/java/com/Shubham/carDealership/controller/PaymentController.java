// src/main/java/com/Shubham/carDealership/controller/PaymentController.java
package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.model.Car;
import com.Shubham.carDealership.model.Order;
import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.CarRepository;
import com.Shubham.carDealership.repository.OrderRepository;
import com.Shubham.carDealership.repository.TradeInRepository;
import com.Shubham.carDealership.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PaymentController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private TradeInRepository tradeInRepository;

    @Value("${payment.mode:mock}")
    private String paymentMode;

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

    @PostMapping("/mock-pay")
    public ResponseEntity<?> mockPayment(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        try {
            Long orderId = Long.parseLong(request.get("orderId").toString());
            String paymentMethod = (String) request.getOrDefault("paymentMethod", "MOCK");

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Order not found"));
            }

            if (!order.getUserId().equals(user.getId())) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Unauthorized"));
            }

            if ("PAID".equals(order.getStatus())) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Order already paid"));
            }

            order.setStatus("PAID");
            order.setPaymentMethod(paymentMethod);
            order.setUpdatedAt(LocalDateTime.now());
            order.setPaymentIntentId("mock_" + UUID.randomUUID().toString());
            orderRepository.save(order);

            Car car = carRepository.findById(order.getCarId()).orElse(null);
            if (car != null) {
                car.setStatus("SOLD");
                car.setUpdatedAt(LocalDateTime.now());
                carRepository.save(car);

                if (order.getTradeInId() != null) {
                    TradeIn tradeIn = tradeInRepository.findById(order.getTradeInId()).orElse(null);
                    if (tradeIn != null) {
                        tradeIn.setStatus("COMPLETED");
                        tradeIn.setUpdatedAt(LocalDateTime.now());
                        tradeInRepository.save(tradeIn);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment successful! Car has been marked as SOLD.");
            response.put("orderId", orderId);
            response.put("order", order);
            response.put("amount", order.getFinalPrice());
            response.put("carId", car != null ? car.getId() : null);
            response.put("carMake", car != null ? car.getMake() : null);
            response.put("carModel", car != null ? car.getModel() : null);
            response.put("carYear", car != null ? car.getYear() : null);
            response.put("paymentMethod", paymentMethod);
            response.put("redirectTo", "/purchase-success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/bank-details")
    public ResponseEntity<?> getBankDetails() {
        Map<String, Object> bankDetails = new HashMap<>();
        bankDetails.put("accountName", "Shubham's Car Dealership (DEMO MODE)");
        bankDetails.put("accountNumber", "12-3456-7891234-00");
        bankDetails.put("bankName", "ASB Bank");
        bankDetails.put("branch", "Auckland CBD");
        bankDetails.put("reference", "Use your ORDER ID as reference");
        bankDetails.put("message", "This is a DEMO - No actual payment required");
        bankDetails.put("mockMode", true);

        return ResponseEntity.ok(Map.of("success", true, "bankDetails", bankDetails));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Order not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", order.getStatus());
        response.put("orderId", orderId);
        response.put("amount", order.getFinalPrice());
        response.put("paymentMethod", order.getPaymentMethod());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/approve/{orderId}")
    public ResponseEntity<?> adminApprovePayment(
            @PathVariable Long orderId,
            HttpServletRequest httpRequest) {

        User admin = getAuthenticatedUser(httpRequest);
        if (admin == null || (!"ADMIN".equals(admin.getRole()) && !"SUPER_ADMIN".equals(admin.getRole()))) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Order not found"));
        }

        if ("PAID".equals(order.getStatus())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Order already approved"));
        }

        order.setStatus("PAID");
        order.setPaymentMethod("ADMIN_APPROVED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        Car car = carRepository.findById(order.getCarId()).orElse(null);
        if (car != null) {
            car.setStatus("SOLD");
            car.setUpdatedAt(LocalDateTime.now());
            carRepository.save(car);
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Order approved and car marked as SOLD"));
    }
}