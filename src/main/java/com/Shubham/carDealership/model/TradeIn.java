// src/main/java/com/Shubham/carDealership/model/TradeIn.java
package com.Shubham.carDealership.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_ins")
@Data
public class TradeIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "car_id")
    private Long carId;

    @Column(name = "rego", nullable = false, unique = true)
    private String rego;

    @Column(name = "trade_make", nullable = false)
    private String tradeMake;

    @Column(name = "trade_model", nullable = false)
    private String tradeModel;

    @Column(name = "trade_year")
    private Integer tradeYear;

    @Column(name = "trade_mileage")
    private Integer tradeMileage;

    @Column(name = "trade_condition")
    private String tradeCondition;

    @Column(name = "estimated_value")
    private BigDecimal estimatedValue;

    @Column(name = "final_value")
    private BigDecimal finalValue;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}