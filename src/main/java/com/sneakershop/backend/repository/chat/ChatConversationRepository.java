package com.sneakershop.backend.repository.chat;

import com.sneakershop.backend.entity.chat.ChatConversation;
import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findFirstByCustomerIdAndDeletedFalseOrderByUpdatedAtDesc(Long customerId);

    @Query("""
        SELECT c FROM ChatConversation c
        LEFT JOIN FETCH c.customer customer
        LEFT JOIN FETCH customer.user customerUser
        LEFT JOIN FETCH c.assignedStaff staff
        WHERE c.deleted = false
        AND (:status IS NULL OR c.status = :status)
        AND (:unreadOnly = false OR c.staffUnreadCount > 0)
        AND (
            :kw IS NULL OR :kw = ''
            OR LOWER(customer.ten) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(customer.email) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR customer.phone LIKE CONCAT('%', :kw, '%')
            OR LOWER(c.lastMessage) LIKE LOWER(CONCAT('%', :kw, '%'))
        )
        ORDER BY c.lastMessageAt DESC, c.updatedAt DESC
    """)
    List<ChatConversation> searchAdmin(@Param("status") ChatConversationStatus status,
                                       @Param("kw") String keyword,
                                       @Param("unreadOnly") boolean unreadOnly);
}
