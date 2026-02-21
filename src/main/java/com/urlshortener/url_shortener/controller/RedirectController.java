package com.urlshortener.url_shortener.controller;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.service.UrlShortenerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {
	
	private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{shortKey}")
    public ResponseEntity<Void> redirect(@PathVariable String shortKey) {
        UrlMapping mapping = service.resolveShortKey(shortKey);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getLongUrl()))
                .build();
    }

}