package com.urlshortener.url_shortener.service;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.repository.UrlMappingRepository;
import com.urlshortener.url_shortener.exception.*;
import com.urlshortener.url_shortener.util.Base62Encoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;


@Service
public class UrlShortenerServiceImpl implements UrlShortenerService{
	private final UrlMappingRepository repository;

    public UrlShortenerServiceImpl(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UrlMapping shortenUrl(String longUrl, LocalDateTime expiresAt) {
        validateUrl(longUrl);

        
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setExpiresAt(expiresAt);
        mapping.setIsActive(true);

        UrlMapping saved = repository.save(mapping);

        
        String shortKey = Base62Encoder.encode(saved.getId());
        saved.setShortKey(shortKey);

       
        return repository.save(saved);
    }

    @Override
    @Transactional
    public UrlMapping resolveShortKey(String shortKey) {
        UrlMapping mapping = repository
                .findByShortKeyAndIsActiveTrue(shortKey)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        
        if (mapping.getExpiresAt() != null &&
                mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("Short URL has expired");
        }
        
        mapping.setClickCount(mapping.getClickCount() + 1);
        repository.save(mapping);

        return mapping;
    }

    private void validateUrl(String url) {
        try {
        	URI uri = URI.create(url);

        	if (uri.getScheme() == null || uri.getHost() == null) {
        	    throw new InvalidUrlException("Invalid URL format");
        	}

        } catch (Exception e) {
            throw new InvalidUrlException("Invalid URL format");
        }
    }
}
