package com.urlshortener.url_shortener.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "click_event")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_key", nullable = false, length = 10)
    private String shortKey;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    
    public ClickEvent() {}

    public ClickEvent(String shortKey, LocalDateTime clickedAt) {
        this.shortKey = shortKey;
        this.clickedAt = clickedAt;
    }

    public Long getId() { return id; }
    public String getShortKey() { return shortKey; }
    public LocalDateTime getClickedAt() { return clickedAt; }
}