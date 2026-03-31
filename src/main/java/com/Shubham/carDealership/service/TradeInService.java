// src/main/java/com/Shubham/carDealership/service/TradeInService.java
package com.Shubham.carDealership.service;

import com.Shubham.carDealership.model.TradeIn;
import com.Shubham.carDealership.repository.TradeInRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradeInService {

    @Autowired
    private TradeInRepository tradeInRepository;

    @Autowired
    private MLTradeInService mlTradeInService;

    // Calculate estimated value using ML with all features
    public BigDecimal calculateEstimatedValueFull(String make, String model, Integer year, Integer mileage, String condition,
                                                  String bodyType, String fuelType, String transmission, Integer owners, Double engineSize) {
        return mlTradeInService.predictValue(
                make, model, year, mileage, condition,
                bodyType, fuelType, transmission, owners, engineSize
        );
    }

    // Simple estimate (for backward compatibility)
    public BigDecimal calculateEstimatedValue(String make, String model, Integer year, Integer mileage, String condition) {
        // Default values for missing features
        String bodyType = "Sedan";
        String fuelType = "Petrol";
        String transmission = "Automatic";
        int owners = 1;
        double engineSize = 2.0;

        return mlTradeInService.predictValue(
                make, model, year, mileage, condition,
                bodyType, fuelType, transmission, owners, engineSize
        );
    }

    public boolean regoExists(String rego) {
        return tradeInRepository.existsByRego(rego.toUpperCase());
    }

    public TradeIn getTradeInByRego(String rego) {
        return tradeInRepository.findByRego(rego.toUpperCase()).orElse(null);
    }

    public TradeIn createTradeIn(Long userId, Long carId, String rego, String make, String model,
                                 Integer year, Integer mileage, String condition, String notes) {
        if (regoExists(rego)) {
            throw new RuntimeException("A trade-in request for this vehicle (Rego: " + rego + ") already exists.");
        }

        // Calculate estimated value using ML
        BigDecimal estimatedValue = calculateEstimatedValue(make, model, year, mileage, condition);

        TradeIn tradeIn = new TradeIn();
        tradeIn.setUserId(userId);
        tradeIn.setCarId(carId);
        tradeIn.setRego(rego.toUpperCase());
        tradeIn.setTradeMake(make);
        tradeIn.setTradeModel(model);
        tradeIn.setTradeYear(year);
        tradeIn.setTradeMileage(mileage);
        tradeIn.setTradeCondition(condition);
        tradeIn.setNotes(notes);
        tradeIn.setEstimatedValue(estimatedValue);
        tradeIn.setStatus("PENDING");
        tradeIn.setCreatedAt(LocalDateTime.now());
        tradeIn.setUpdatedAt(LocalDateTime.now());

        return tradeInRepository.save(tradeIn);
    }

    public TradeIn approveTradeIn(Long tradeInId, BigDecimal finalValue) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setFinalValue(finalValue);
            tradeIn.setStatus("APPROVED");
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }

    public TradeIn rejectTradeIn(Long tradeInId, String reason) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setStatus("REJECTED");
            tradeIn.setNotes(reason);
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }

    public List<TradeIn> getUserTradeIns(Long userId) {
        return tradeInRepository.findByUserId(userId);
    }

    public List<TradeIn> getPendingTradeIns() {
        return tradeInRepository.findByStatus("PENDING");
    }

    public TradeIn completeTradeIn(Long tradeInId) {
        TradeIn tradeIn = tradeInRepository.findById(tradeInId).orElse(null);
        if (tradeIn != null) {
            tradeIn.setStatus("COMPLETED");
            tradeIn.setUpdatedAt(LocalDateTime.now());
            return tradeInRepository.save(tradeIn);
        }
        return null;
    }
}