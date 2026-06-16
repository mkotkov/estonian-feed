package com.estonianfeed.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String url;

    private String source;

    private LocalDateTime publishedAt;

    private boolean sent = false;

    // Constructurs 
    public Article() {}

    public Article(String title, String url, String source, LocalDateTime publishedAt) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}