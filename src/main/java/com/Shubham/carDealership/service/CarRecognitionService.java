package com.Shubham.carDealership.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class CarRecognitionService {

    @Value("${ml.carrecognition.url:http://localhost:5002}")
    private String mlServerUrl;

    @Value("${ml.carrecognition.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean serverAvailable = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            System.out.println("⚠️ Car Recognition service is disabled");
            return;
        }

        // Check if ML server is available
        try {
            URL url = new URL(mlServerUrl + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                serverAvailable = true;
                System.out.println("✅ Car Recognition ML Server is available at: " + mlServerUrl);

                // Get list of brands
                URL brandsUrl = new URL(mlServerUrl + "/brands");
                HttpURLConnection brandsConn = (HttpURLConnection) brandsUrl.openConnection();
                brandsConn.setRequestMethod("GET");

                if (brandsConn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(brandsConn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonNode json = objectMapper.readTree(response.toString());
                    if (json.has("brands")) {
                        System.out.println("   Recognizable brands: " + json.get("brands"));
                    }
                }
            } else {
                System.out.println("⚠️ Car Recognition ML Server returned: " + responseCode);
                serverAvailable = false;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Car Recognition ML Server not available: " + e.getMessage());
            serverAvailable = false;
        }
    }

    /**
     * Predict car brand from image
     */
    public CarRecognitionResult predictCar(MultipartFile image) {
        if (!enabled || !serverAvailable) {
            // Return mock result if server is not available
            return getMockPrediction();
        }

        try {
            // Prepare multipart form data
            String boundary = "----" + System.currentTimeMillis();
            String lineEnd = "\r\n";

            URL url = new URL(mlServerUrl + "/predict");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // Write image to request body
            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream), true);

            // Add image part
            writer.append("--" + boundary).append(lineEnd);
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"" + image.getOriginalFilename() + "\"").append(lineEnd);
            writer.append("Content-Type: " + image.getContentType()).append(lineEnd);
            writer.append(lineEnd);
            writer.flush();

            // Write image bytes
            outputStream.write(image.getBytes());
            outputStream.flush();

            writer.append(lineEnd);
            writer.flush();

            writer.append("--" + boundary + "--").append(lineEnd);
            writer.close();

            // Get response
            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonNode json = objectMapper.readTree(response.toString());

                if (json.has("success") && json.get("success").asBoolean()) {
                    CarRecognitionResult result = new CarRecognitionResult();
                    result.setSuccess(true);
                    result.setPredictedBrand(json.get("predicted_brand").asText());
                    result.setConfidence(json.get("confidence").asDouble());

                    if (json.has("top_3")) {
                        List<CarRecognitionResult.BrandConfidence> top3 = new ArrayList<>();
                        for (JsonNode item : json.get("top_3")) {
                            CarRecognitionResult.BrandConfidence bc = new CarRecognitionResult.BrandConfidence();
                            bc.setBrand(item.get("brand").asText());
                            bc.setConfidence(item.get("confidence").asDouble());
                            top3.add(bc);
                        }
                        result.setTop3(top3);
                    }

                    return result;
                } else {
                    String error = json.has("error") ? json.get("error").asText() : "Unknown error";
                    CarRecognitionResult result = new CarRecognitionResult();
                    result.setSuccess(false);
                    result.setError(error);
                    return result;
                }
            } else {
                CarRecognitionResult result = new CarRecognitionResult();
                result.setSuccess(false);
                result.setError("ML server returned error code: " + responseCode);
                return result;
            }

        } catch (Exception e) {
            CarRecognitionResult result = new CarRecognitionResult();
            result.setSuccess(false);
            result.setError("Failed to connect to ML server: " + e.getMessage());
            return result;
        }
    }

    /**
     * Get mock prediction for testing
     */
    private CarRecognitionResult getMockPrediction() {
        CarRecognitionResult result = new CarRecognitionResult();
        result.setSuccess(true);
        result.setPredictedBrand("BMW");
        result.setConfidence(87.5);
        result.setMock(true);

        List<CarRecognitionResult.BrandConfidence> top3 = new ArrayList<>();

        CarRecognitionResult.BrandConfidence bc1 = new CarRecognitionResult.BrandConfidence();
        bc1.setBrand("BMW");
        bc1.setConfidence(87.5);
        top3.add(bc1);

        CarRecognitionResult.BrandConfidence bc2 = new CarRecognitionResult.BrandConfidence();
        bc2.setBrand("Audi");
        bc2.setConfidence(8.2);
        top3.add(bc2);

        CarRecognitionResult.BrandConfidence bc3 = new CarRecognitionResult.BrandConfidence();
        bc3.setBrand("Mercedes");
        bc3.setConfidence(4.3);
        top3.add(bc3);

        result.setTop3(top3);

        return result;
    }

    public boolean isServerAvailable() {
        return enabled && serverAvailable;
    }

    /**
     * Inner class for recognition result
     */
    public static class CarRecognitionResult {
        private boolean success;
        private String predictedBrand;
        private double confidence;
        private List<BrandConfidence> top3;
        private String error;
        private boolean mock;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getPredictedBrand() { return predictedBrand; }
        public void setPredictedBrand(String predictedBrand) { this.predictedBrand = predictedBrand; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public List<BrandConfidence> getTop3() { return top3; }
        public void setTop3(List<BrandConfidence> top3) { this.top3 = top3; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public boolean isMock() { return mock; }
        public void setMock(boolean mock) { this.mock = mock; }

        public static class BrandConfidence {
            private String brand;
            private double confidence;

            public String getBrand() { return brand; }
            public void setBrand(String brand) { this.brand = brand; }

            public double getConfidence() { return confidence; }
            public void setConfidence(double confidence) { this.confidence = confidence; }
        }
    }
}