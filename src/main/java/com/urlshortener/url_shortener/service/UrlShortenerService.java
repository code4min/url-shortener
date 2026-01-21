package com.urlshortener.url_shortener.service;
import com.urlshortener.url_shortener.model.UrlMapping;
import java.time.LocalDateTime;

public interface UrlShortenerService {
	UrlMapping shortenUrl(String longUrl, LocalDateTime expiresAt);

    UrlMapping resolveShortKey(String shortKey);

}
