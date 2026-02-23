package com.urlshortener.url_shortener.interceptor;

import com.urlshortener.url_shortener.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor{
	
	 private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
	    private static final String KEY_PREFIX = "rate:";

	    private final StringRedisTemplate redisTemplate;
	    private final int maxRequests;
	    private final long windowSeconds;

	    public RateLimitInterceptor(
	            StringRedisTemplate redisTemplate,
	            @Value("${app.rate-limit.max-requests:5}") int maxRequests,
	            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
	        this.redisTemplate = redisTemplate;
	        this.maxRequests = maxRequests;
	        this.windowSeconds = windowSeconds;
	    }

	    @Override
	    public boolean preHandle(HttpServletRequest request,
	                             HttpServletResponse response,
	                             Object handler) {

	        String ip = extractClientIp(request);
	        String key = buildKey(ip);

	        // Increment the counter — if key doesn't exist, Redis creates it starting at 1
	        Long count = redisTemplate.opsForValue().increment(key);

	        if (count == null) {
	            // Redis unavailable — fail open (allow request) to avoid blocking legitimate users
	            log.warn("Redis unavailable during rate limit check for IP '{}', failing open", ip);
	            return true;
	        }

	        // On first increment, set the expiry window
	        if (count == 1) {
	            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
	            log.debug("Rate limit window started for IP '{}', key='{}'", ip, key);
	        }

	        log.debug("Rate limit check — IP='{}' count={}/{} window={}s", ip, count, maxRequests, windowSeconds);

	        if (count > maxRequests) {
	            log.warn("Rate limit exceeded for IP '{}' — {} requests in window", ip, count);
	            throw new TooManyRequestsException(
	                "Rate limit exceeded: max " + maxRequests + " requests per " + windowSeconds + " seconds"
	            );
	        }

	        return true;
	    }

	    private String extractClientIp(HttpServletRequest request) {
	        // Check X-Forwarded-For header first (set by proxies/load balancers)
	        String forwarded = request.getHeader("X-Forwarded-For");
	        if (forwarded != null && !forwarded.isBlank()) {
	            // X-Forwarded-For can be a comma-separated list — first IP is the real client
	            return forwarded.split(",")[0].trim();
	        }
	        return request.getRemoteAddr();
	    }

	    private String buildKey(String ip) {
	        // Window slot = current epoch second divided by window size
	        // This gives the same number for all requests within the same window
	        long windowSlot = System.currentTimeMillis() / 1000 / windowSeconds;
	        return KEY_PREFIX + ip + ":" + windowSlot;
	    }

}
