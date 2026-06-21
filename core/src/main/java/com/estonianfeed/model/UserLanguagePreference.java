package com.estonianfeed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_language_preferences")
public class UserLanguagePreference {

    @Id
    private Long chatId;

    @Column(nullable = false)
    private String language; // ET, RU, EN

    public UserLanguagePreference() {}

    public UserLanguagePreference(Long chatId, String language) {
        this.chatId = chatId;
        this.language = language;
    }

    public Long getChatId() { return chatId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}