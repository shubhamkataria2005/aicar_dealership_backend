package com.Shubham.carDealership.repository;

import com.Shubham.carDealership.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId);
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :user1Id AND m.receiverId = :user2Id) " +
            "OR (m.senderId = :user2Id AND m.receiverId = :user1Id) ORDER BY m.createdAt ASC")
    List<Message> findConversation(Long user1Id, Long user2Id);

    @Query("SELECT m FROM Message m WHERE m.receiverId = :receiverId AND m.isRead = false")
    List<Message> findUnreadMessages(Long receiverId);
}