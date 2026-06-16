package com.estonianfeed.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String company;

    @Column(nullable = false, unique = true)
    private String url;

    private String location;

    private LocalDateTime publishedAt;

    private boolean sent = false;

    public Job() {}

    public Job(String title, String company, String url, String location, LocalDateTime publishedAt) {
        this.title = title;
        this.company = company;
        this.url = url;
        this.location = location;
        this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }
}