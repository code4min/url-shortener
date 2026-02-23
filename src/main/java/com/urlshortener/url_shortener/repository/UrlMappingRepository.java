package com.urlshortener.url_shortener.repository;

import com.urlshortener.url_shortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortKeyAndIsActiveTrue(String shortKey);
    
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortKey = :shortKey AND u.isActive = true")
    void incrementClickCount(@Param("shortKey") String shortKey);
    
    // Fetch all expired but still active URLs — we need shortKeys for Redis eviction
    @Query("SELECT u FROM UrlMapping u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    List<UrlMapping> findExpiredActiveUrls(@Param("now") LocalDateTime now);

    // Bulk deactivate all expired URLs in one UPDATE
    @Modifying
    @Query("UPDATE UrlMapping u SET u.isActive = false WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);
}