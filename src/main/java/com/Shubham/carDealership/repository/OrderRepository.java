// src/main/java/com/Shubham/carDealership/repository/OrderRepository.java
package com.Shubham.carDealership.repository;

import com.Shubham.carDealership.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByCarId(Long carId);
}