package com.sneakershop.backend.controller.chat;

import com.sneakershop.backend.dto.chat.ChatConversationResponse;
import com.sneakershop.backend.dto.chat.ChatMessageResponse;
import com.sneakershop.backend.dto.chat.SendChatMessageRequest;
import com.sneakershop.backend.dto.chat.UpdateChatConversationStatusRequest;
import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import com.sneakershop.backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/chat")
public class AdminChatController {
    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ChatConversationResponse>> conversations(
            @RequestParam(required = false) ChatConversationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        return ResponseEntity.ok(chatService.listAdmin(status, keyword, unreadOnly));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ChatConversationResponse> detail(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.detailAdmin(conversationId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> messages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.listMessagesForAdmin(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponse> sendText(@PathVariable Long conversationId,
                                                        @RequestBody SendChatMessageRequest request) {
        return new ResponseEntity<>(chatService.sendAdminText(conversationId, request), HttpStatus.CREATED);
    }

    @PostMapping(value = "/conversations/{conversationId}/images", consumes = "multipart/form-data")
    public ResponseEntity<ChatMessageResponse> sendImage(@PathVariable Long conversationId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "content", required = false) String content) {
        return new ResponseEntity<>(chatService.sendAdminImage(conversationId, content, file), HttpStatus.CREATED);
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long conversationId) {
        chatService.markAdminRead(conversationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/conversations/{conversationId}/status")
    public ResponseEntity<ChatConversationResponse> updateStatus(@PathVariable Long conversationId,
                                                                 @RequestBody UpdateChatConversationStatusRequest request) {
        return ResponseEntity.ok(chatService.updateAdminStatus(conversationId, request));
    }
}
