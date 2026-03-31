// src/main/java/com/Shubham/carDealership/controller/CarController.java
package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.dto.CarRequest;
import com.Shubham.carDealership.dto.CarResponse;
import com.Shubham.carDealership.model.Car;
import com.Shubham.carDealership.model.Order;
import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.CarRepository;
import com.Shubham.carDealership.repository.OrderRepository;
import com.Shubham.carDealership.repository.TradeInRepository;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class CarController {

    @Autowired
    private CarService carService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeInRepository tradeInRepository;

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

    @PostMapping("/list")
    public ResponseEntity<?> listCar(@RequestBody CarRequest request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.ok(response);
        }

        CarResponse car = carService.listCar(request, user);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("car", car);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dealership/add")
    public ResponseEntity<?> addDealershipCar(@RequestBody CarRequest request, HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);

        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.ok(response);
        }

        boolean isEmployeeOrAdmin = "SALES_EMPLOYEE".equals(user.getRole()) ||
                "ADMIN".equals(user.getRole()) ||
                "SUPER_ADMIN".equals(user.getRole()) ||
                (user.getIsEmployee() != null && user.getIsEmployee());

        if (!isEmployeeOrAdmin) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Only sales employees can add dealership cars");
            return ResponseEntity.ok(response);
        }

        try {
            CarResponse car = carService.addDealershipCar(request, user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("car", car);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add dealership car: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PutMapping("/admin/cars/{carId}/inspection")
    public ResponseEntity<?> updateInspectionStatus(@PathVariable Long carId,
                                                    @RequestBody Map<String, String> payload,
                                                    HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);

        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Car not found"));
        }

        String newStatus = payload.get("inspectionStatus");
        if (!List.of("PENDING", "PASSED", "FAILED").contains(newStatus)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid inspection status"));
        }

        car.setInspectionStatus(newStatus);
        car.setUpdatedAt(LocalDateTime.now());
        carRepository.save(car);

        return ResponseEntity.ok(Map.of("success", true, "message", "Inspection status updated to " + newStatus));
    }

    @PostMapping("/purchase/{carId}")
    public ResponseEntity<?> purchaseCar(@PathVariable Long carId,
                                         @RequestBody Map<String, Object> request,
                                         HttpServletRequest httpRequest) {
        User user = getAuthenticatedUser(httpRequest);
        if (user == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Please login first"));
        }

        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Car not found"));
        }

        if (!"AVAILABLE".equals(car.getStatus())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Car is no longer available"));
        }

        BigDecimal finalPrice = car.getPrice();
        Long tradeInId = request.get("tradeInId") != null ? Long.valueOf(request.get("tradeInId").toString()) : null;
        BigDecimal tradeInValue = BigDecimal.ZERO;

        if (tradeInId != null) {
            TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
            if (tradeIn != null && "APPROVED".equals(tradeIn.getStatus())) {
                tradeInValue = tradeIn.getFinalValue();
                finalPrice = finalPrice.subtract(tradeInValue);
                if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                    finalPrice = BigDecimal.ZERO;
                }
                tradeIn.setStatus("COMPLETED");
                tradeIn.setUpdatedAt(LocalDateTime.now());
                tradeInRepository.save(tradeIn);
            }
        }

        car.setStatus("SOLD");
        car.setUpdatedAt(LocalDateTime.now());
        carRepository.save(car);

        Order order = new Order();
        order.setUserId(user.getId());
        order.setCarId(carId);
        order.setOriginalPrice(car.getPrice());
        order.setTradeInId(tradeInId);
        order.setTradeInValue(tradeInValue);
        order.setFinalPrice(finalPrice);
        order.setStatus("COMPLETED");
        order.setPaymentMethod(request.get("paymentMethod") != null ? (String) request.get("paymentMethod") : "CARD");
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Purchase successful!");
        response.put("originalPrice", car.getPrice());
        response.put("tradeInValue", tradeInValue);
        response.put("finalPrice", finalPrice);
        response.put("orderId", order.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCars() {
        List<CarResponse> cars = carService.getAllAvailableCars();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dealership/inventory")
    public ResponseEntity<?> getDealershipInventory() {
        List<CarResponse> cars = carService.getDealershipInventory();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        response.put("source", "DEALERSHIP");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/marketplace/listings")
    public ResponseEntity<?> getMarketplaceListings() {
        List<CarResponse> cars = carService.getMarketplaceListings();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        response.put("source", "MARKETPLACE");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-cars")
    public ResponseEntity<?> getMyCars(HttpServletRequest request) {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.ok(response);
        }

        List<CarResponse> cars = carService.getUserCars(user.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCarById(@PathVariable Long id) {
        try {
            CarResponse car = carService.getCarById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("car", car);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Car not found");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCars(
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String bodyType,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String carSource) {

        List<CarResponse> cars = carService.searchCars(make, bodyType, fuel, maxPrice, carSource);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        return ResponseEntity.ok(response);
    }
}