package com.sneakershop.backend.entity.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.chat.enums.ChatConversationStatus;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
@Data
@ToString(exclude = {"messages", "customer", "assignedStaff"})
@EqualsAndHashCode(exclude = {"messages", "customer", "assignedStaff"})
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChatConversationStatus status = ChatConversationStatus.OPEN;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "customer_unread_count", nullable = false)
    private Integer customerUnreadCount = 0;

    @Column(name = "staff_unread_count", nullable = false)
    private Integer staffUnreadCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ChatConversationStatus.OPEN;
        if (customerUnreadCount == null) customerUnreadCount = 0;
        if (staffUnreadCount == null) staffUnreadCount = 0;
        if (deleted == null) deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
