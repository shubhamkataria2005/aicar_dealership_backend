package com.Shubham.carDealership.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private Long senderId;
    private Long receiverId;
    private Long carId;
    private String content;
}