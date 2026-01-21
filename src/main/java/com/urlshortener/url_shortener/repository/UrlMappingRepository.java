package com.urlshortener.url_shortener.repository;

import com.urlshortener.url_shortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortKeyAndIsActiveTrue(String shortKey);
}


