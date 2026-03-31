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
    private String status = "COMPLETED";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}