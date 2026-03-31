// src/main/java/com/Shubham/carDealership/repository/TradeInRepository.java
package com.Shubham.carDealership.repository;

import com.Shubham.carDealership.model.TradeIn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TradeInRepository extends JpaRepository<TradeIn, Long> {
    List<TradeIn> findByUserId(Long userId);
    List<TradeIn> findByStatus(String status);
    List<TradeIn> findByUserIdAndStatus(Long userId, String status);
    Optional<TradeIn> findByRego(String rego);
    boolean existsByRego(String rego);
}