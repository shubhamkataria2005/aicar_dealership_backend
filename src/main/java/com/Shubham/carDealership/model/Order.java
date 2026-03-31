// src/main/java/com/Shubham/carDealership/model/Order.java
package com.Shubham.carDealership.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "trade_in_id")
    private Long tradeInId;

    @Column(name = "trade_in_value")
    private BigDecimal tradeInValue;

    @Column(name = "final_price", nullable = false)
    private BigDecimal finalPrice;

    @Column(name = "status")
    private String status = "PENDING_PAYMENT";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "customer_first_name")
    private String customerFirstName;

    @Column(name = "customer_last_name")
    private String customerLastName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "customer_city")
    private String customerCity;

    @Column(name = "customer_postcode")
    private String customerPostcode;

    @Column(name = "delivery_method")
    private String deliveryMethod;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "special_instructions")
    private String specialInstructions;
}