package com.sneakershop.backend.repository.chat;

import com.sneakershop.backend.entity.chat.ChatMessage;
import com.sneakershop.backend.entity.chat.enums.ChatSenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(Long conversationId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.conversation.id = :conversationId AND m.senderType <> :readerType")
    int markReadByOppositeSide(@Param("conversationId") Long conversationId, @Param("readerType") ChatSenderType readerType);
}
