// src/main/java/com/Shubham/carDealership/model/ServiceAppointment.java
package com.Shubham.carDealership.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_appointments")
@Data
public class ServiceAppointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id")
    private Long carId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "service_type", nullable = false)
    private String serviceType; // TEST_DRIVE, INSPECTION, MAINTENANCE, REPAIR

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(name = "technician_id")
    private Long technicianId;

    private String status = "SCHEDULED"; // SCHEDULED, CONFIRMED, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}