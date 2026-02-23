package com.urlshortener.url_shortener.config;

import com.urlshortener.url_shortener.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
	
	private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering RateLimitInterceptor for /api/shorten");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/shorten");
    }

}
