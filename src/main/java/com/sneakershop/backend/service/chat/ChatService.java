package com.sneakershop.backend.service.chat;

import com.sneakershop.backend.dto.chat.ChatConversationResponse;
import com.sneakershop.backend.dto.chat.ChatMessageResponse;
import com.sneakershop.backend.dto.chat.SendChatMessageRequest;
import com.sneakershop.backend.dto.chat.UpdateChatConversationStatusRequest;
import com.sneakershop.backend.entity.chat.ChatConversation;
import com.sneakershop.backend.entity.chat.ChatMessage;
import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import com.sneakershop.backend.entity.chat.enums.ChatMessageType;
import com.sneakershop.backend.entity.chat.enums.ChatSenderType;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.chat.ChatConversationRepository;
import com.sneakershop.backend.repository.chat.ChatMessageRepository;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final int MAX_TEXT_LENGTH = 1000;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Value("${upload.path}")
    private String uploadPathConfig;

    @Transactional
    public ChatConversationResponse getOrCreateCurrentCustomerConversation() {
        Customer customer = currentCustomerOrCreate();
        ChatConversation conversation = conversationRepository
                .findFirstByCustomerIdAndDeletedFalseOrderByUpdatedAtDesc(customer.getId())
                .orElseGet(() -> createConversation(customer));
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            conversation.setStatus(ChatConversationStatus.OPEN);
        }
        return toConversationResponse(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse getCurrentCustomerConversation() {
        Customer customer = currentCustomerOrCreate();
        ChatConversation conversation = conversationRepository
                .findFirstByCustomerIdAndDeletedFalseOrderByUpdatedAtDesc(customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bạn chưa có cuộc trò chuyện nào."));
        return toConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> listAdmin(ChatConversationStatus status, String keyword, Boolean unreadOnly) {
        return conversationRepository.searchAdmin(status, safeTrim(keyword), Boolean.TRUE.equals(unreadOnly))
                .stream()
                .map(this::toConversationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatConversationResponse detailAdmin(Long conversationId) {
        return toConversationResponse(findConversation(conversationId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessagesForCustomer(Long conversationId) {
        ChatConversation conversation = findConversation(conversationId);
        ensureOwnConversation(conversation);
        return messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessagesForAdmin(Long conversationId) {
        findConversation(conversationId);
        return messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendCustomerText(Long conversationId, SendChatMessageRequest request) {
        return sendCustomerMessage(conversationId, request != null ? request.getContent() : null, null);
    }

    @Transactional
    public ChatMessageResponse sendCustomerImage(Long conversationId, String content, MultipartFile file) {
        String imageUrl = storeChatImage(file);
        return sendCustomerMessage(conversationId, content, imageUrl);
    }

    @Transactional
    public ChatMessageResponse sendAdminText(Long conversationId, SendChatMessageRequest request) {
        return sendAdminMessage(conversationId, request != null ? request.getContent() : null, null);
    }

    @Transactional
    public ChatMessageResponse sendAdminImage(Long conversationId, String content, MultipartFile file) {
        String imageUrl = storeChatImage(file);
        return sendAdminMessage(conversationId, content, imageUrl);
    }

    @Transactional
    public void markCustomerRead(Long conversationId) {
        ChatConversation conversation = findConversation(conversationId);
        ensureOwnConversation(conversation);
        messageRepository.markReadByOppositeSide(conversationId, ChatSenderType.CUSTOMER);
        conversation.setCustomerUnreadCount(0);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void markAdminRead(Long conversationId) {
        ChatConversation conversation = findConversation(conversationId);
        messageRepository.markReadByOppositeSide(conversationId, ChatSenderType.ADMIN);
        conversation.setStaffUnreadCount(0);
        conversationRepository.save(conversation);
    }

    @Transactional
    public ChatConversationResponse updateAdminStatus(Long conversationId, UpdateChatConversationStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn trạng thái cuộc trò chuyện.");
        }
        ChatConversation conversation = findConversation(conversationId);
        conversation.setStatus(request.getStatus());
        return toConversationResponse(conversationRepository.save(conversation));
    }

    private ChatMessageResponse sendCustomerMessage(Long conversationId, String content, String imageUrl) {
        ChatConversation conversation = findConversation(conversationId);
        ensureOwnConversation(conversation);
        Customer customer = currentCustomerOrCreate();
        ChatMessage message = buildMessage(conversation, ChatSenderType.CUSTOMER, customer.getId(), content, imageUrl);
        if (conversation.getStatus() == ChatConversationStatus.CLOSED || conversation.getStatus() == ChatConversationStatus.RESOLVED) {
            conversation.setStatus(ChatConversationStatus.OPEN);
        } else {
            conversation.setStatus(ChatConversationStatus.PENDING);
        }
        conversation.setStaffUnreadCount(nz(conversation.getStaffUnreadCount()) + 1);
        updateLastMessage(conversation, message);
        conversationRepository.save(conversation);
        return toMessageResponse(messageRepository.save(message));
    }

    private ChatMessageResponse sendAdminMessage(Long conversationId, String content, String imageUrl) {
        ChatConversation conversation = findConversation(conversationId);
        User staff = currentUserOrThrow();
        ChatSenderType senderType = hasRole(staff, "ADMIN") ? ChatSenderType.ADMIN : ChatSenderType.STAFF;
        ChatMessage message = buildMessage(conversation, senderType, staff.getId(), content, imageUrl);
        if (conversation.getAssignedStaff() == null) {
            conversation.setAssignedStaff(staff);
        }
        conversation.setStatus(ChatConversationStatus.OPEN);
        conversation.setCustomerUnreadCount(nz(conversation.getCustomerUnreadCount()) + 1);
        updateLastMessage(conversation, message);
        conversationRepository.save(conversation);
        return toMessageResponse(messageRepository.save(message));
    }

    private ChatMessage buildMessage(ChatConversation conversation, ChatSenderType senderType, Long senderId, String content, String imageUrl) {
        String cleanContent = safeTrim(content);
        if (cleanContent != null && cleanContent.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung tin nhắn không được vượt quá 1000 ký tự.");
        }
        if ((cleanContent == null || cleanContent.isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gửi tin nhắn rỗng.");
        }
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(cleanContent);
        message.setImageUrl(imageUrl);
        if (imageUrl != null && cleanContent != null && !cleanContent.isEmpty()) {
            message.setMessageType(ChatMessageType.TEXT_IMAGE);
        } else if (imageUrl != null) {
            message.setMessageType(ChatMessageType.IMAGE);
        } else {
            message.setMessageType(ChatMessageType.TEXT);
        }
        message.setRead(false);
        return message;
    }

    private void updateLastMessage(ChatConversation conversation, ChatMessage message) {
        String text = safeTrim(message.getContent());
        if (text == null || text.isEmpty()) {
            text = message.getImageUrl() != null ? "[Hình ảnh]" : "Tin nhắn mới";
        }
        conversation.setLastMessage(text.length() > 240 ? text.substring(0, 240) + "..." : text);
        conversation.setLastMessageAt(LocalDateTime.now());
    }

    private ChatConversation createConversation(Customer customer) {
        ChatConversation conversation = new ChatConversation();
        conversation.setCustomer(customer);
        conversation.setStatus(ChatConversationStatus.OPEN);
        conversation.setLastMessage("Cuộc trò chuyện mới");
        conversation.setLastMessageAt(LocalDateTime.now());
        return conversationRepository.save(conversation);
    }

    private String storeChatImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh cần gửi.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh chat không được vượt quá 5MB.");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0 && idx < original.length() - 1) ext = original.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ cho phép gửi ảnh jpg, jpeg, png hoặc webp.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File gửi lên không phải là ảnh hợp lệ.");
        }
        try {
            String folder = "chat/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path uploadDir = Paths.get(uploadPathConfig).resolve(folder);
            Files.createDirectories(uploadDir);
            String fileName = UUID.randomUUID() + "." + ext;
            Path target = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "uploads/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload ảnh chat thất bại: " + ex.getMessage());
        }
    }

    private ChatConversation findConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc trò chuyện."));
    }

    private void ensureOwnConversation(ChatConversation conversation) {
        Customer customer = currentCustomerOrCreate();
        if (conversation.getCustomer() == null || !conversation.getCustomer().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem cuộc trò chuyện này.");
        }
    }

    private Customer currentCustomerOrCreate() {
        User user = currentUserOrThrow();
        if (user.getCustomer() != null) return user.getCustomer();
        return customerRepository.findByUserId(user.getId()).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setTen(firstNonBlank(user.getFullName(), user.getUsername(), user.getEmail(), "Khách hàng"));
            customer.setEmail(user.getEmail());
            customer.setStatus("ACTIVE");
            customer.setDiemTichLuy(0);
            customer.setLoaiKhach("BRONZE");
            customer.setUser(user);
            return customerRepository.save(customer);
        });
    }

    private User currentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để chat với shop.");
        }
        String key = auth.getName();
        return userRepository.findByUsername(key)
                .or(() -> userRepository.findByEmail(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản hiện tại."));
    }

    private ChatConversationResponse toConversationResponse(ChatConversation c) {
        ChatConversationResponse dto = new ChatConversationResponse();
        dto.setId(c.getId());
        if (c.getCustomer() != null) {
            dto.setCustomerId(c.getCustomer().getId());
            dto.setCustomerName(c.getCustomer().getTen());
            dto.setCustomerEmail(c.getCustomer().getEmail());
            dto.setCustomerPhone(c.getCustomer().getPhone());
        }
        if (c.getAssignedStaff() != null) {
            dto.setAssignedStaffId(c.getAssignedStaff().getId());
            dto.setAssignedStaffName(firstNonBlank(c.getAssignedStaff().getFullName(), c.getAssignedStaff().getUsername(), c.getAssignedStaff().getEmail(), "Nhân viên"));
        }
        dto.setStatus(c.getStatus());
        dto.setLastMessage(c.getLastMessage());
        dto.setLastMessageAt(c.getLastMessageAt());
        dto.setCustomerUnreadCount(nz(c.getCustomerUnreadCount()));
        dto.setStaffUnreadCount(nz(c.getStaffUnreadCount()));
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(m.getId());
        dto.setConversationId(m.getConversation() != null ? m.getConversation().getId() : null);
        dto.setSenderType(m.getSenderType());
        dto.setSenderId(m.getSenderId());
        dto.setSenderName(resolveSenderName(m));
        dto.setMessageType(m.getMessageType());
        dto.setContent(m.getContent());
        dto.setImageUrl(m.getImageUrl());
        dto.setRead(m.getRead());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }

    private String resolveSenderName(ChatMessage m) {
        if (m.getSenderType() == ChatSenderType.CUSTOMER && m.getConversation() != null && m.getConversation().getCustomer() != null) {
            return m.getConversation().getCustomer().getTen();
        }
        if (m.getSenderId() != null && (m.getSenderType() == ChatSenderType.ADMIN || m.getSenderType() == ChatSenderType.STAFF)) {
            return userRepository.findById(m.getSenderId())
                    .map(u -> firstNonBlank(u.getFullName(), u.getUsername(), u.getEmail(), "Nhân viên"))
                    .orElse("Nhân viên");
        }
        return "Hệ thống";
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(r -> roleName.equalsIgnoreCase(r.getCode()) || roleName.equalsIgnoreCase(r.getName()));
    }

    private Integer nz(Integer value) { return value == null ? 0 : value; }

    private String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String clean = safeTrim(value);
            if (clean != null) return clean;
        }
        return "";
    }
}
