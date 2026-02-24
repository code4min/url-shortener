package com.urlshortener.url_shortener.schedular;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.repository.UrlMappingRepository;
import com.urlshortener.url_shortener.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class UrlExpirySchedular {

    private static final Logger log = LoggerFactory.getLogger(UrlExpirySchedular.class);

    private final UrlMappingRepository repository;
    private final CacheService cacheService;

    public UrlExpirySchedular(UrlMappingRepository repository, CacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedRateString = "${app.expiry.check-interval-ms:300000}")
    @Transactional
    public void processExpiredUrls() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Expiry job triggered at {}", now);

        
        List<UrlMapping> expired = repository.findExpiredActiveUrls(now);

        if (expired.isEmpty()) {
            log.debug("Expiry job — no expired URLs found");
            return;
        }

        log.info("Expiry job — found {} expired URL(s), processing...", expired.size());

       
        expired.forEach(mapping -> {
            cacheService.evict(mapping.getShortKey());
            log.debug("Evicted Redis key for shortKey='{}'", mapping.getShortKey());
        });

        
        int deactivated = repository.deactivateExpiredUrls(now);
        log.info("Expiry job — deactivated {} URL(s) in DB", deactivated);
    }
}