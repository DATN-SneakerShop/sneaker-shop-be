package com.sneakershop.backend.controller.chat;

import com.sneakershop.backend.dto.chat.ChatConversationResponse;
import com.sneakershop.backend.dto.chat.ChatMessageResponse;
import com.sneakershop.backend.dto.chat.SendChatMessageRequest;
import com.sneakershop.backend.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/customer")
public class CustomerChatController {
    private final ChatService chatService;

    @PostMapping("/conversations/current")
    public ResponseEntity<ChatConversationResponse> getOrCreateCurrentConversation() {
        return new ResponseEntity<>(chatService.getOrCreateCurrentCustomerConversation(), HttpStatus.CREATED);
    }

    @GetMapping("/conversations/current")
    public ResponseEntity<ChatConversationResponse> getCurrentConversation() {
        return ResponseEntity.ok(chatService.getCurrentCustomerConversation());
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> messages(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.listMessagesForCustomer(conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponse> sendText(@PathVariable Long conversationId,
                                                        @RequestBody SendChatMessageRequest request) {
        return new ResponseEntity<>(chatService.sendCustomerText(conversationId, request), HttpStatus.CREATED);
    }

    @PostMapping(value = "/conversations/{conversationId}/images", consumes = "multipart/form-data")
    public ResponseEntity<ChatMessageResponse> sendImage(@PathVariable Long conversationId,
                                                         @RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "content", required = false) String content) {
        return new ResponseEntity<>(chatService.sendCustomerImage(conversationId, content, file), HttpStatus.CREATED);
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long conversationId) {
        chatService.markCustomerRead(conversationId);
        return ResponseEntity.noContent().build();
    }
}
