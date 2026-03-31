package com.Shubham.carDealership.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIAssistantService {

    @Autowired
    private OpenAIService openAIService;

    public String getResponse(String userMessage) {
        return openAIService.generateResponse(userMessage);
    }
}