// src/main/java/com/Shubham/carDealership/repository/ServiceAppointmentRepository.java
package com.Shubham.carDealership.repository;

import com.Shubham.carDealership.model.ServiceAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceAppointmentRepository extends JpaRepository<ServiceAppointment, Long> {
    List<ServiceAppointment> findByUserId(Long userId);
    List<ServiceAppointment> findByTechnicianId(Long technicianId);
    List<ServiceAppointment> findByStatus(String status);
}