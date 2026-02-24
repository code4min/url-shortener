package com.urlshortener.url_shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UrlAnalyticsResponse {
	private String shortKey;
    private String longUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private long totalClicks;
    private long clicksLast24Hours;
    private long clicksLast7Days;
    private List<DailyClickCount> clicksPerDay;

    @Getter
    @AllArgsConstructor
    public static class DailyClickCount {
        private LocalDate date;
        private long count;
    }
}
