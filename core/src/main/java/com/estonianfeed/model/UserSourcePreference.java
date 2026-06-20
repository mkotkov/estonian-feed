package com.estonianfeed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_source_preferences")
public class UserSourcePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String sourceId;

    public UserSourcePreference() {}

    public UserSourcePreference(Long chatId, String sourceId) {
        this.chatId = chatId;
        this.sourceId = sourceId;
    }

    public Long getId() { return id; }
    public Long getChatId() { return chatId; }
    public String getSourceId() { return sourceId; }
}