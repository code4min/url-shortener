package com.urlshortener.url_shortener.service;

import com.urlshortener.url_shortener.dto.UrlAnalyticsResponse;

public interface AnalyticsService {
	
	void recordClick(String shortKey);
	
    UrlAnalyticsResponse getAnalytics(String shortKey);

}
