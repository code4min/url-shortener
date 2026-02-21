package com.urlshortener.url_shortener.service;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {
	

    
    void put(String shortKey, String longUrl, Duration ttl);

    
    Optional<String> get(String shortKey);

   
    void evict(String shortKey);

}
