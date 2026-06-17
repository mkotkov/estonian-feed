package com.estonianfeed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_subscriptions")
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String keyword;

    public UserSubscription() {}

    public UserSubscription(Long chatId, String keyword) {
        this.chatId = chatId;
        this.keyword = keyword.toLowerCase().trim();
    }

    public Long getId() { return id; }
    public Long getChatId() { return chatId; }
    public String getKeyword() { return keyword; }
}