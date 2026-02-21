package com.urlshortener.url_shortener.service;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.repository.UrlMappingRepository;
import com.urlshortener.url_shortener.exception.*;
import com.urlshortener.url_shortener.util.Base62Encoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Service
public class UrlShortenerServiceImpl implements UrlShortenerService{
	
	private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);
	
	private final UrlMappingRepository repository;

	private final CacheService cacheService;

    public UrlShortenerServiceImpl(UrlMappingRepository repository, CacheService cacheService) {
        this.repository = repository;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    public UrlMapping shortenUrl(String longUrl, LocalDateTime expiresAt) {
        validateUrl(longUrl);

        
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new InvalidUrlException("Expiry date must be in the future");
        }

        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setExpiresAt(expiresAt);
        mapping.setIsActive(true);

        UrlMapping saved = repository.save(mapping);

        String shortKey = Base62Encoder.encode(saved.getId());
        saved.setShortKey(shortKey);
        UrlMapping finalSaved = repository.save(saved);

        // Prime the cache on creation so first redirect is also a cache hit
        Duration ttl = computeTtl(finalSaved);
        cacheService.put(shortKey, longUrl, ttl);
        log.debug("Cached '{}' → '{}' with TTL {}", shortKey, longUrl, ttl);

        return finalSaved;
    }

    @Override
    @Transactional
    public UrlMapping resolveShortKey(String shortKey) {

        // 1. Check cache first
        Optional<String> cachedUrl = cacheService.get(shortKey);
        log.debug("Cache lookup for key '{}': {}", shortKey, cachedUrl.isPresent() ? "HIT" : "MISS");

        if (cachedUrl.isPresent()) {
            log.debug("Serving '{}' from cache", shortKey);
            // Single UPDATE, no SELECT — no need to fetch the full entity just to increment
            repository.incrementClickCount(shortKey);

            UrlMapping cached = new UrlMapping();
            cached.setLongUrl(cachedUrl.get());
            cached.setShortKey(shortKey);
            return cached;
        }

        // 2. Cache miss — hit the database
        log.debug("Cache miss — querying database for key '{}'", shortKey);
        UrlMapping mapping = repository
                .findByShortKeyAndIsActiveTrue(shortKey)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found"));

        // 3. Check expiry
        if (mapping.getExpiresAt() != null &&
                mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            cacheService.evict(shortKey); // defensive eviction
            throw new UrlExpiredException("Short URL has expired");
        }

        // 4. Increment click count
        mapping.setClickCount(mapping.getClickCount() + 1);
        repository.save(mapping);

        // 5. Populate cache for future requests
        Duration ttl = computeTtl(mapping);
        cacheService.put(shortKey, mapping.getLongUrl(), ttl);
        log.debug("Cached '{}' → '{}' with TTL {}", shortKey, mapping.getLongUrl(), ttl);

        return mapping;
    }
    
    private Duration computeTtl(UrlMapping mapping) {
        if (mapping.getExpiresAt() != null) {
            Duration ttl = Duration.between(LocalDateTime.now(), mapping.getExpiresAt());
            return ttl.isNegative() ? Duration.ZERO : ttl;
        }
        // null means no expiry — CacheService will use its default TTL
        return null;
    }

    private void validateUrl(String url) {
    	  try {
              URI uri = URI.create(url);
              String scheme = uri.getScheme();

              if (scheme == null || uri.getHost() == null) {
                  throw new InvalidUrlException("Invalid URL format");
              }

              if (!scheme.equals("http") && !scheme.equals("https")) {
                  throw new InvalidUrlException("Only http and https URLs are supported");
              }

          } catch (IllegalArgumentException e) {
              throw new InvalidUrlException("Invalid URL format");
          }
      }
}