package com.urlshortener.url_shortener.controller;

import com.urlshortener.url_shortener.dto.UrlAnalyticsResponse;
import com.urlshortener.url_shortener.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
	
	 private final AnalyticsService analyticsService;

	    public AnalyticsController(AnalyticsService analyticsService) {
	        this.analyticsService = analyticsService;
	    }

	    @GetMapping("/{shortKey}")
	    public ResponseEntity<UrlAnalyticsResponse> getAnalytics(@PathVariable String shortKey) {
	        return ResponseEntity.ok(analyticsService.getAnalytics(shortKey));
	    }

}
