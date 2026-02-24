package com.urlshortener.url_shortener.service;

import com.urlshortener.url_shortener.dto.UrlAnalyticsResponse;
import com.urlshortener.url_shortener.exception.UrlNotFoundException;
import com.urlshortener.url_shortener.model.ClickEvent;
import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.repository.ClickEventRepository;
import com.urlshortener.url_shortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService{
	
	private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final ClickEventRepository clickEventRepository;
    private final UrlMappingRepository urlMappingRepository;

    public AnalyticsServiceImpl(ClickEventRepository clickEventRepository,
                                 UrlMappingRepository urlMappingRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlMappingRepository = urlMappingRepository;
    }

    @Override
    @Async
    @Transactional
    public void recordClick(String shortKey) {
        try {
            
            clickEventRepository.save(new ClickEvent(shortKey, LocalDateTime.now()));

            
            urlMappingRepository.incrementClickCount(shortKey);

            log.debug("Recorded click for shortKey='{}'", shortKey);
        } catch (Exception e) {
            
            log.error("Failed to record click for shortKey='{}': {}", shortKey, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UrlAnalyticsResponse getAnalytics(String shortKey) {
        UrlMapping mapping = urlMappingRepository
        		.findByShortKeyAndIsActiveTrue(shortKey)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for key: " + shortKey));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minus24Hours = now.minusHours(24);
        LocalDateTime minus7Days = now.minusDays(7);

        long totalClicks = mapping.getClickCount();
        long clicksLast24Hours = clickEventRepository
                .countByShortKeyAndClickedAtAfter(shortKey, minus24Hours);
        long clicksLast7Days = clickEventRepository
                .countByShortKeyAndClickedAtAfter(shortKey, minus7Days);

        List<UrlAnalyticsResponse.DailyClickCount> clicksPerDay = clickEventRepository
                .getDailyClickCounts(shortKey, minus7Days)
                .stream()
                .map(row -> new UrlAnalyticsResponse.DailyClickCount(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue()
                ))
                .toList();

        return new UrlAnalyticsResponse(
                shortKey,
                mapping.getLongUrl(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.getIsActive(),
                totalClicks,
                clicksLast24Hours,
                clicksLast7Days,
                clicksPerDay
        );
    }

    private Long getIdByShortKey(String shortKey) {
        return urlMappingRepository
                .findByShortKeyAndIsActiveTrue(shortKey)
                .map(UrlMapping::getId)
                .orElseThrow(() -> new UrlNotFoundException("URL not found for key: " + shortKey));
    }

    private LocalDate toLocalDate(Object obj) {
        
        if (obj instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (obj instanceof LocalDate localDate) {
            return localDate;
        }
        throw new IllegalArgumentException("Cannot convert to LocalDate: " + obj.getClass());
    }

}
