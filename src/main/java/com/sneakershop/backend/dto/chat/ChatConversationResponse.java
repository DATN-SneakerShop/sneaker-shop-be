package com.sneakershop.backend.dto.chat;

import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Long assignedStaffId;
    private String assignedStaffName;
    private ChatConversationStatus status;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer customerUnreadCount;
    private Integer staffUnreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
