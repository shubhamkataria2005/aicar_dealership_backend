package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.service.AIAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-assistant")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AIAssistantController {

    @Autowired
    private AIAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Message cannot be empty"));
        }

        String response = aiAssistantService.getResponse(userMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);

        return ResponseEntity.ok(result);
    }
}