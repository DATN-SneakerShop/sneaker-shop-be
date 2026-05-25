package com.sneakershop.backend.dto.chat;

import com.sneakershop.backend.entity.chat.enums.ChatMessageType;
import com.sneakershop.backend.entity.chat.enums.ChatSenderType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private ChatSenderType senderType;
    private Long senderId;
    private String senderName;
    private ChatMessageType messageType;
    private String content;
    private String imageUrl;
    private Boolean read;
    private LocalDateTime createdAt;
}
