// src/main/java/com/Shubham/carDealership/service/MLTradeInService.java
package com.Shubham.carDealership.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MLTradeInService {

    @Value("${ml.tradein.url:http://localhost:5003}")
    private String mlServerUrl;

    @Value("${ml.tradein.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean serverAvailable = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            System.out.println("⚠️ Trade-In ML service is disabled");
            return;
        }

        try {
            URL url = new URL(mlServerUrl + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                serverAvailable = true;
                System.out.println("✅ Trade-In ML Server is available at: " + mlServerUrl);
            } else {
                System.out.println("⚠️ Trade-In ML Server returned: " + responseCode);
                serverAvailable = false;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Trade-In ML Server not available: " + e.getMessage());
            serverAvailable = false;
        }
    }

    public BigDecimal predictValue(String make, String model, int year, int mileage,
                                   String condition, String bodyType, String fuelType,
                                   String transmission, int owners, double engineSize) {

        if (!enabled || !serverAvailable) {
            System.out.println("⚠️ Using fallback calculation (ML server not available)");
            return calculateFallbackValue(make, model, year, mileage, condition);
        }

        try {
            System.out.println("🤖 Calling Trade-In ML server for prediction...");
            System.out.println("   URL: " + mlServerUrl + "/predict");
            System.out.println("   Input: " + make + " " + model + " " + year + " " + mileage + "km");

            Map<String, Object> request = new HashMap<>();
            request.put("make", make);
            request.put("model", model);
            request.put("year", year);
            request.put("mileage", mileage);
            request.put("condition", condition);
            request.put("body_type", bodyType);
            request.put("fuel_type", fuelType);
            request.put("transmission", transmission);
            request.put("owners", owners);
            request.put("engine_size", engineSize);

            String requestJson = objectMapper.writeValueAsString(request);

            URL url = new URL(mlServerUrl + "/predict");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // Write request
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestJson.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);

                    if (result.containsKey("success") && (boolean) result.get("success")) {
                        double value = ((Number) result.get("predicted_value")).doubleValue();
                        System.out.println("✅ ML prediction successful: $" + value);
                        return BigDecimal.valueOf(value);
                    } else {
                        String error = result.containsKey("message") ? (String) result.get("message") : "Unknown error";
                        throw new RuntimeException(error);
                    }
                }
            } else {
                throw new RuntimeException("ML server returned error code: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("❌ ML prediction failed: " + e.getMessage());
            return calculateFallbackValue(make, model, year, mileage, condition);
        }
    }

    private BigDecimal calculateFallbackValue(String make, String model, int year, int mileage, String condition) {
        System.out.println("📊 Using fallback calculation for: " + make + " " + model);

        Map<String, Double> brandValues = new HashMap<>();
        brandValues.put("Toyota", 28000.0);
        brandValues.put("Honda", 26000.0);
        brandValues.put("Mazda", 27000.0);
        brandValues.put("Subaru", 29000.0);
        brandValues.put("Nissan", 25000.0);
        brandValues.put("Ford", 30000.0);
        brandValues.put("BMW", 55000.0);
        brandValues.put("Mercedes", 58000.0);
        brandValues.put("Audi", 52000.0);
        brandValues.put("Hyundai", 24000.0);
        brandValues.put("Kia", 23500.0);
        brandValues.put("Volkswagen", 31000.0);
        brandValues.put("Tesla", 65000.0);
        brandValues.put("Porsche", 120000.0);
        brandValues.put("Ferrari", 400000.0);
        brandValues.put("Lamborghini", 350000.0);
        brandValues.put("Chevrolet", 32000.0);
        brandValues.put("Dodge", 35000.0);
        brandValues.put("Jeep", 33000.0);
        brandValues.put("Volvo", 38000.0);
        brandValues.put("Lexus", 45000.0);
        brandValues.put("Jaguar", 48000.0);
        brandValues.put("Land Rover", 52000.0);
        brandValues.put("Mitsubishi", 22000.0);

        double baseValue = brandValues.getOrDefault(make, 25000.0);
        int age = 2025 - year;
        double ageFactor = Math.max(0.3, 1.0 - (age * 0.10));
        double mileageFactor = Math.max(0.6, 1.0 - (mileage / 200000.0));

        Map<String, Double> conditionFactors = new HashMap<>();
        conditionFactors.put("Excellent", 1.0);
        conditionFactors.put("Very Good", 0.9);
        conditionFactors.put("Good", 0.8);
        conditionFactors.put("Fair", 0.65);
        conditionFactors.put("Poor", 0.5);
        double conditionFactor = conditionFactors.getOrDefault(condition, 0.7);

        double estimatedValue = baseValue * ageFactor * mileageFactor * conditionFactor;
        estimatedValue = Math.max(1000, Math.min(150000, estimatedValue));
        estimatedValue = Math.round(estimatedValue / 100) * 100;

        System.out.println("   Fallback value: $" + estimatedValue);
        return BigDecimal.valueOf(estimatedValue);
    }

    public boolean isModelAvailable() {
        return enabled && serverAvailable;
    }
}