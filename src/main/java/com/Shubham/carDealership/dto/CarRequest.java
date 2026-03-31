package com.Shubham.carDealership.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CarRequest {
    private String make;
    private String model;
    private Integer year;
    private BigDecimal price;
    private Integer mileage;
    private String fuel;
    private String transmission;
    private String bodyType;
    private String description;
    private String imageUrl;
}