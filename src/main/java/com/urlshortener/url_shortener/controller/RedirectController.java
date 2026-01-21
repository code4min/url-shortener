package com.urlshortener.url_shortener.controller;

import com.urlshortener.url_shortener.model.UrlMapping;
import com.urlshortener.url_shortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {
	
	private final UrlShortenerService service;

    public RedirectController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/{shortKey}")
    public void redirect(@PathVariable String shortKey,
                         HttpServletResponse response) throws IOException {

        UrlMapping mapping = service.resolveShortKey(shortKey);

        response.setStatus(HttpServletResponse.SC_FOUND); 
        response.setHeader("Location", mapping.getLongUrl());
    }

}
