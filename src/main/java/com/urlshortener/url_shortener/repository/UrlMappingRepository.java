package com.urlshortener.url_shortener.repository;

import com.urlshortener.url_shortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortKeyAndIsActiveTrue(String shortKey);
    
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortKey = :shortKey AND u.isActive = true")
    void incrementClickCount(@Param("shortKey") String shortKey);
}