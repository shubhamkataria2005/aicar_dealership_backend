// src/main/java/com/Shubham/carDealership/controller/AdminController.java
package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.dto.UserDto;
import com.Shubham.carDealership.model.Car;
import com.Shubham.carDealership.model.Message;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.CarRepository;
import com.Shubham.carDealership.repository.MessageRepository;
import com.Shubham.carDealership.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private MessageRepository messageRepository;

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

    private boolean isSuperAdmin(User user) {
        return user != null && "SUPER_ADMIN".equals(user.getRole());
    }

    private boolean isAdmin(User user) {
        return user != null && ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()));
    }

    // ── Dashboard Statistics ────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        Map<String, Object> stats = new HashMap<>();
        List<User> allUsers = userRepository.findAll();

        stats.put("totalUsers", allUsers.size());
        stats.put("totalCars", carRepository.count());
        stats.put("availableCars", carRepository.findByStatus("AVAILABLE").size());
        stats.put("soldCars", carRepository.findByStatus("SOLD").size());
        stats.put("totalMessages", messageRepository.count());
        stats.put("adminRole", admin.getRole());

        // Count users by role
        stats.put("regularUsers", allUsers.stream().filter(u -> "USER".equals(u.getRole())).count());
        stats.put("salesEmployees", allUsers.stream().filter(u -> "SALES_EMPLOYEE".equals(u.getRole())).count());
        stats.put("admins", allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count());
        stats.put("superAdmins", allUsers.stream().filter(u -> "SUPER_ADMIN".equals(u.getRole())).count());

        // Recent users (last 5)
        stats.put("recentUsers", allUsers.stream()
                .limit(5)
                .map(this::mapToUserDto)
                .collect(Collectors.toList()));

        // Recent cars (last 5)
        stats.put("recentCars", carRepository.findAll().stream()
                .limit(5)
                .collect(Collectors.toList()));

        stats.put("success", true);
        return ResponseEntity.ok(stats);
    }

    // ── Get all users ──────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        List<UserDto> users = userRepository.findAll().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("users", users);
        response.put("currentUserRole", admin.getRole());
        response.put("currentUserId", admin.getId());
        return ResponseEntity.ok(response);
    }

    // ── Update user role (Only Super Admin) ─────────────────────────────────
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId,
                                            @RequestBody Map<String, String> payload,
                                            HttpServletRequest request) {
        User currentAdmin = getAuthenticatedUser(request);

        if (!isSuperAdmin(currentAdmin)) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Only Super Admins can change user roles"
            ));
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "User not found"));
        }

        String newRole = payload.get("role");
        // UPDATED: Added SALES_EMPLOYEE to allowed roles
        if (!List.of("USER", "ADMIN", "SUPER_ADMIN", "SALES_EMPLOYEE").contains(newRole)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid role"));
        }

        // Prevent demoting the last super admin
        if ("SUPER_ADMIN".equals(targetUser.getRole()) && !"SUPER_ADMIN".equals(newRole)) {
            long superAdminCount = userRepository.findAll().stream()
                    .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                    .count();
            if (superAdminCount <= 1) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Cannot demote the last Super Admin"
                ));
            }
        }

        targetUser.setRole(newRole);
        // Update is_employee flag based on role
        targetUser.setIsEmployee("SALES_EMPLOYEE".equals(newRole) || "ADMIN".equals(newRole) || "SUPER_ADMIN".equals(newRole));
        userRepository.save(targetUser);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User role updated successfully",
                "newRole", newRole
        ));
    }

    // ── Delete user ────────────────────────────────────────────────────────
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        User currentAdmin = getAuthenticatedUser(request);

        if (!isAdmin(currentAdmin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        if (currentAdmin.getId().equals(userId)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Cannot delete your own account"));
        }

        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "User not found"));
        }

        // Check permissions
        if (!isSuperAdmin(currentAdmin) && !"USER".equals(targetUser.getRole())) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "You don't have permission to delete this user"
            ));
        }

        // Prevent deleting the last super admin
        if ("SUPER_ADMIN".equals(targetUser.getRole())) {
            long superAdminCount = userRepository.findAll().stream()
                    .filter(u -> "SUPER_ADMIN".equals(u.getRole()))
                    .count();
            if (superAdminCount <= 1) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Cannot delete the last Super Admin"
                ));
            }
        }

        // Delete user's cars
        List<Car> userCars = carRepository.findBySellerId(userId);
        carRepository.deleteAll(userCars);

        userRepository.delete(targetUser);

        return ResponseEntity.ok(Map.of("success", true, "message", "User deleted successfully"));
    }

    // ── Get all cars (admin view) ──────────────────────────────────────────
    @GetMapping("/cars")
    public ResponseEntity<?> getAllCarsAdmin(HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        List<Car> cars = carRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cars", cars);
        response.put("adminRole", admin.getRole());
        return ResponseEntity.ok(response);
    }

    // ── Update car status ──────────────────────────────────────────────────
    @PutMapping("/cars/{carId}/status")
    public ResponseEntity<?> updateCarStatus(@PathVariable Long carId,
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

        String newStatus = payload.get("status");
        if (!List.of("AVAILABLE", "SOLD", "PENDING", "REJECTED").contains(newStatus)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid status"));
        }

        car.setStatus(newStatus);
        carRepository.save(car);

        return ResponseEntity.ok(Map.of("success", true, "message", "Car status updated successfully"));
    }

    // ── Delete car (admin) ─────────────────────────────────────────────────
    @DeleteMapping("/cars/{carId}")
    public ResponseEntity<?> deleteCar(@PathVariable Long carId, HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Car not found"));
        }

        carRepository.delete(car);
        return ResponseEntity.ok(Map.of("success", true, "message", "Car deleted successfully"));
    }

    // ── Get all messages (admin view) ──────────────────────────────────────
    @GetMapping("/messages")
    public ResponseEntity<?> getAllMessages(HttpServletRequest request) {
        User admin = getAuthenticatedUser(request);
        if (!isAdmin(admin)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Admin access required"));
        }

        List<Message> messages = messageRepository.findAll();

        // Enrich messages with sender/receiver names
        List<Map<String, Object>> enrichedMessages = messages.stream().map(msg -> {
            Map<String, Object> enriched = new HashMap<>();
            enriched.put("id", msg.getId());
            enriched.put("content", msg.getContent());
            enriched.put("senderId", msg.getSenderId());
            enriched.put("receiverId", msg.getReceiverId());
            enriched.put("carId", msg.getCarId());
            enriched.put("isRead", msg.getIsRead());
            enriched.put("createdAt", msg.getCreatedAt());

            User sender = userRepository.findById(msg.getSenderId()).orElse(null);
            User receiver = userRepository.findById(msg.getReceiverId()).orElse(null);

            enriched.put("senderName", sender != null ? sender.getUsername() : "Unknown");
            enriched.put("receiverName", receiver != null ? receiver.getUsername() : "Unknown");

            return enriched;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("messages", enrichedMessages);
        return ResponseEntity.ok(response);
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}