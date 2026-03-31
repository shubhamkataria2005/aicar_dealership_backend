package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.service.CarRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/car-recognition")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class CarRecognitionController {

    @Autowired
    private CarRecognitionService carRecognitionService;

    /**
     * Upload a car image and get brand prediction
     */
    @PostMapping("/predict")
    public ResponseEntity<?> predictCarBrand(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Please select an image");
            return ResponseEntity.ok(response);
        }

        // Check file type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "File must be an image");
            return ResponseEntity.ok(response);
        }

        // Check file size (max 10MB)
        if (image.getSize() > 10 * 1024 * 1024) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Image size must be less than 10MB");
            return ResponseEntity.ok(response);
        }

        CarRecognitionService.CarRecognitionResult result = carRecognitionService.predictCar(image);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());

        if (result.isSuccess()) {
            response.put("predictedBrand", result.getPredictedBrand());
            response.put("confidence", result.getConfidence());
            response.put("top3", result.getTop3());
            response.put("mock", result.isMock());
            response.put("message", "Prediction completed successfully");
        } else {
            response.put("error", result.getError());
            response.put("message", "Prediction failed");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Check if ML server is available
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("available", carRecognitionService.isServerAvailable());
        response.put("service", "car_recognition");
        return ResponseEntity.ok(response);
    }
}