package com.sneakershop.backend.dto.chat;

import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import lombok.Data;

@Data
public class UpdateChatConversationStatusRequest {
    private ChatConversationStatus status;
}
