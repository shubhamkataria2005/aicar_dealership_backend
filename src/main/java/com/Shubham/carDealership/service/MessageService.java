package com.Shubham.carDealership.service;

import com.Shubham.carDealership.model.Message;
import com.Shubham.carDealership.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public List<Message> getConversation(Long user1Id, Long user2Id) {
        return messageRepository.findConversation(user1Id, user2Id);
    }

    public List<Message> getUnreadMessages(Long userId) {
        return messageRepository.findUnreadMessages(userId);
    }

    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message != null) {
            message.setIsRead(true);
            messageRepository.save(message);
        }
    }
}