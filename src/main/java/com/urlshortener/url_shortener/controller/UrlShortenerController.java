package com.urlshortener.url_shortener.controller;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.dto.ShortenUrlRequest;
import com.urlshortener.url_shortener.dto.ShortenUrlResponse;
import com.urlshortener.url_shortener.service.UrlShortenerService;
import  jakarta.validation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")

public class UrlShortenerController {
	
	private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    public ShortenUrlResponse shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {

        UrlMapping mapping = service.shortenUrl(
                request.getLongUrl(),
                request.getExpiresAt()
        );

        String shortUrl = "http://localhost:8080/" + mapping.getShortKey();
        return new ShortenUrlResponse(shortUrl);
    }

}
