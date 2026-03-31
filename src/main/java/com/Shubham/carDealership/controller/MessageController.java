package com.Shubham.carDealership.controller;

import com.Shubham.carDealership.config.JwtUtil;
import com.Shubham.carDealership.dto.MessageRequest;
import com.Shubham.carDealership.model.Message;
import com.Shubham.carDealership.model.User;
import com.Shubham.carDealership.repository.MessageRepository;
import com.Shubham.carDealership.repository.UserRepository;
import com.Shubham.carDealership.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    // ── Helper: get user from JWT ──────────────────────────────────────────
    private User getAuthenticatedUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.extractUserId(token);
                return userRepository.findById(userId).orElse(null);
            }
        }
        return null;
    }

    // ── REST: Send a message ───────────────────────────────────────────────
    // Called from CarDetailPage when buyer clicks "Send Message"
    @PostMapping("/api/messages/send")
    @ResponseBody
    public ResponseEntity<?> sendMessageRest(
            @RequestBody MessageRequest messageRequest,
            HttpServletRequest httpRequest) {

        User sender = getAuthenticatedUser(httpRequest);
        if (sender == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Please login to send messages");
            return ResponseEntity.ok(err);
        }

        // Save to database
        Message message = new Message();
        message.setSenderId(sender.getId());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setCarId(messageRequest.getCarId());
        message.setContent(messageRequest.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        // Also push via WebSocket so receiver gets it in real-time
        Map<String, Object> wsPayload = buildMessagePayload(saved, sender);
        messagingTemplate.convertAndSendToUser(
                messageRequest.getReceiverId().toString(),
                "/queue/messages",
                wsPayload
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", saved);
        return ResponseEntity.ok(response);
    }

    // ── REST: Get conversation between two users ───────────────────────────
    @GetMapping("/api/messages/conversation/{otherUserId}")
    @ResponseBody
    public ResponseEntity<?> getConversation(
            @PathVariable Long otherUserId,
            HttpServletRequest httpRequest) {

        User me = getAuthenticatedUser(httpRequest);
        if (me == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Please login");
            return ResponseEntity.ok(err);
        }

        List<Message> messages = messageRepository.findConversation(me.getId(), otherUserId);

        // Mark messages sent to me as read
        messages.stream()
                .filter(m -> m.getReceiverId().equals(me.getId()) && !m.getIsRead())
                .forEach(m -> {
                    m.setIsRead(true);
                    messageRepository.save(m);
                });

        // Enrich with sender names
        List<Map<String, Object>> enriched = messages.stream()
                .map(m -> {
                    User sender = userRepository.findById(m.getSenderId()).orElse(null);
                    return buildMessagePayload(m, sender);
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("messages", enriched);
        return ResponseEntity.ok(response);
    }

    // ── REST: Get all conversations (inbox) ────────────────────────────────
    // Returns one entry per unique contact, with latest message and unread count
    @GetMapping("/api/messages/inbox")
    @ResponseBody
    public ResponseEntity<?> getInbox(HttpServletRequest httpRequest) {

        User me = getAuthenticatedUser(httpRequest);
        if (me == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Please login");
            return ResponseEntity.ok(err);
        }

        // Get all messages where I am sender or receiver
        List<Message> sent     = messageRepository.findBySenderIdOrderByCreatedAtDesc(me.getId());
        List<Message> received = messageRepository.findByReceiverIdOrderByCreatedAtDesc(me.getId());

        // Build a map: otherUserId → latest message + unread count
        Map<Long, Map<String, Object>> conversations = new LinkedHashMap<>();

        List<Message> all = new ArrayList<>();
        all.addAll(sent);
        all.addAll(received);
        all.sort(Comparator.comparing(Message::getCreatedAt).reversed());

        for (Message m : all) {
            Long otherUserId = m.getSenderId().equals(me.getId())
                    ? m.getReceiverId()
                    : m.getSenderId();

            if (!conversations.containsKey(otherUserId)) {
                User other = userRepository.findById(otherUserId).orElse(null);

                // Count unread from this contact
                long unread = received.stream()
                        .filter(r -> r.getSenderId().equals(otherUserId) && !r.getIsRead())
                        .count();

                Map<String, Object> convo = new HashMap<>();
                convo.put("otherUserId",   otherUserId);
                convo.put("otherUsername", other != null ? other.getUsername() : "Unknown");
                convo.put("otherEmail",    other != null ? other.getEmail()    : "");
                convo.put("latestMessage", m.getContent());
                convo.put("latestTime",    m.getCreatedAt());
                convo.put("unreadCount",   unread);
                convo.put("carId",         m.getCarId());

                conversations.put(otherUserId, convo);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success",       true);
        response.put("conversations", new ArrayList<>(conversations.values()));
        return ResponseEntity.ok(response);
    }

    // ── REST: Get unread message count ─────────────────────────────────────
    @GetMapping("/api/messages/unread-count")
    @ResponseBody
    public ResponseEntity<?> getUnreadCount(HttpServletRequest httpRequest) {

        User me = getAuthenticatedUser(httpRequest);
        if (me == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("count", 0);
            return ResponseEntity.ok(result);
        }

        long count = messageRepository.findUnreadMessages(me.getId()).size();
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return ResponseEntity.ok(result);
    }

    // ── WebSocket: real-time send ──────────────────────────────────────────
    @MessageMapping("/chat.send")
    public void sendMessageWs(@Payload MessageRequest messageRequest) {
        Message message = new Message();
        message.setSenderId(messageRequest.getSenderId());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setCarId(messageRequest.getCarId());
        message.setContent(messageRequest.getContent());
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        User sender = userRepository.findById(messageRequest.getSenderId()).orElse(null);
        Map<String, Object> payload = buildMessagePayload(saved, sender);

        messagingTemplate.convertAndSendToUser(
                messageRequest.getReceiverId().toString(), "/queue/messages", payload);
        messagingTemplate.convertAndSendToUser(
                messageRequest.getSenderId().toString(),   "/queue/messages", payload);
    }

    // ── Helper: build response map ─────────────────────────────────────────
    private Map<String, Object> buildMessagePayload(Message m, User sender) {
        Map<String, Object> p = new HashMap<>();
        p.put("id",           m.getId());
        p.put("senderId",     m.getSenderId());
        p.put("senderName",   sender != null ? sender.getUsername() : "Unknown");
        p.put("receiverId",   m.getReceiverId());
        p.put("carId",        m.getCarId());
        p.put("content",      m.getContent());
        p.put("createdAt",    m.getCreatedAt());
        p.put("isRead",       m.getIsRead());
        return p;
    }
}