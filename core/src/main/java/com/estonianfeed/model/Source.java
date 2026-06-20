package com.estonianfeed.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    private String language;

    public Source() {}

    public Source(String id, String name, String url, String language) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.language = language;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getLanguage() { return language; }
}