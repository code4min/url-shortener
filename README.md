# URL Shortener

A production-inspired URL shortening service built with Spring Boot, PostgreSQL, and Redis. Designed in two phases to demonstrate clean architecture, caching strategy, and backend engineering fundamentals.

---

## Tech Stack

- **Java 17** + **Spring Boot 4**
- **PostgreSQL 18** - primary data store
- **Redis 7** - cache layer and rate limiting
- **Hibernate / Spring Data JPA** - ORM and repository layer
- **Lettuce** - Redis client (via Spring Data Redis)
- **Docker** - containerized Postgres and Redis

---

## Features

### Phase 1 - Core
- Shorten a long URL to a Base62-encoded short key
- Redirect via short key with `302 FOUND`
- Optional URL expiry (`expiresAt` field)
- Input validation - HTTP/HTTPS only, no past expiry dates
- Centralized error handling via `@ControllerAdvice`
- Custom exceptions: `UrlNotFoundException`, `UrlExpiredException`, `InvalidUrlException`, `TooManyRequestsException`

### Phase 2 - Performance & Observability
- **Redis cache-aside** on redirect path - cache hit serves redirect with zero DB reads
- **Optimized click counting** - single blind `UPDATE click_count+1` on cache hit, no `SELECT` required
- **Rate limiting** - fixed window counter using Redis `INCR` + `EXPIRE`, applied to `POST /api/shorten`
- **URL expiry scheduler** - background job evicts expired keys from Redis and marks them inactive in DB
- **Async click analytics** - click events recorded in background thread via `@Async`, redirect never waits for analytics writes
- **Analytics endpoint** - total clicks, clicks in last 24 hours, clicks in last 7 days, daily breakdown

---

## Architecture

### Redirect Flow (Cache Hit)
```
GET /{shortKey}
  -> Redis HIT
  -> 302 FOUND returned immediately        <- user gets redirect
  -> @Async thread: INSERT click_event
                   UPDATE click_count+1   <- analytics in background
```

### Redirect Flow (Cache Miss)
```
GET /{shortKey}
  - Redis MISS
  - SELECT from url_mapping
  - expiry check
  - populate Redis cache
  - 302 FOUND
  - @Async thread: INSERT click_event + UPDATE click_count+1
```

### URL Expiry Flow
```
Scheduler (every 5 min)
  -> SELECT expired active URLs
  -> evict each from Redis
  -> bulk UPDATE is_active = false
```

Redis TTL is set to match `expiresAt` at cache population time - expired URLs are auto-evicted by Redis naturally, the scheduler handles the DB cleanup.

### Rate Limiting
Fixed window counter per IP using Redis:
```
key: rate:{ip}:{windowSlot}
value: INCR on each request
TTL: set on first request in window
```
Applied only to `POST /api/shorten`. Redirect traffic is intentionally excluded.

---

## API Reference

### Shorten a URL
```
POST /api/shorten
Content-Type: application/json

{
  "longUrl": "https://www.example.com",
  "expiresAt": "2026-03-01T00:00:00"   
}
```
**Response `201 CREATED`:**
```json
{
  "shortKey": "aB3",
  "shortUrl": "http://localhost:8080/aB3",
  "longUrl": "https://www.example.com",
  "expiresAt": "2026-03-01T00:00:00"
}
```

### Redirect
```
GET /{shortKey}
-> 302 FOUND  (valid URL)
-> 410 GONE   (expired)
-> 404        (not found)
-> 429        (rate limit exceeded on shorten endpoint)
```

### Analytics
```
GET /api/analytics/{shortKey}
```
**Response `200 OK`:**
```json
{
  "shortKey": "aB3",
  "longUrl": "https://www.example.com",
  "createdAt": "2026-02-24T06:12:35",
  "expiresAt": null,
  "isActive": true,
  "totalClicks": 42,
  "clicksLast24Hours": 10,
  "clicksLast7Days": 35,
  "clicksPerDay": [
    { "date": "2026-02-24", "count": 10 },
    { "date": "2026-02-23", "count": 25 }
  ]
}
```

---

## Database Schema

```postgresql
CREATE TABLE url_mapping (
    id          BIGSERIAL PRIMARY KEY,
    short_key   VARCHAR(10) UNIQUE NOT NULL,
    long_url    TEXT NOT NULL,
    click_count BIGINT DEFAULT 0,
    created_at  TIMESTAMP NOT NULL,
    expires_at  TIMESTAMP,
    is_active   BOOLEAN DEFAULT true
);

CREATE TABLE click_event (
    id         BIGSERIAL PRIMARY KEY,
    short_key  VARCHAR(10) NOT NULL,
    clicked_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_click_event_short_key ON click_event(short_key);
CREATE INDEX idx_click_event_clicked_at ON click_event(clicked_at);
```

`url_mapping.click_count` serves as a fast total counter. `click_event` stores individual timestamped events for time-series analytics queries.

---

## Project Structure

```
src/main/java/com/urlshortener/url_shortener/
├── controller/
│   ├── UrlShortenerController.java   # POST /api/shorten
│   ├── RedirectController.java       # GET /{shortKey}
│   └── AnalyticsController.java      # GET /api/analytics/{shortKey}
├── service/
│   ├── UrlShortenerService.java
│   ├── UrlShortenerServiceImpl.java
│   ├── CacheService.java
│   ├── RedisCacheServiceImpl.java
│   ├── AnalyticsService.java
│   └── AnalyticsServiceImpl.java     # @Async click recording
├── scheduler/
│   └── UrlExpiryScheduler.java       # @Scheduled expiry job
├── interceptor/
│   └── RateLimitInterceptor.java     # Redis fixed window rate limiting
├── repository/
│   ├── UrlMappingRepository.java
│   └── ClickEventRepository.java
├── model/
│   ├── UrlMapping.java
│   └── ClickEvent.java
├── dto/
│   ├── ShortenUrlRequest.java
│   ├── ShortenUrlResponse.java
│   └── UrlAnalyticsResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── UrlNotFoundException.java
│   ├── UrlExpiredException.java
│   ├── InvalidUrlException.java
│   └── TooManyRequestsException.java
├── config/
│   ├── RedisConfig.java
│   └── WebMvcConfig.java
└── util/
    └── Base62Encoder.java
```

---

## Key Engineering Decisions

**Cache-aside over write-through** - explicit per-URL TTL control. URLs with `expiresAt` get a Redis TTL matching their expiry exactly. URLs without expiry get a 24-hour default TTL.

**Blind UPDATE for click counts on cache hits** - on a cache hit, there is no `SELECT`. A single `UPDATE click_count = click_count + 1 WHERE short_key = ? AND is_active = true` runs atomically in the background. Zero reads on the hot redirect path.

**Analytics off the critical path** - `analyticsService.recordClick()` is `@Async`. The redirect thread returns `302` before the analytics write begins. A missed click event due to an exception is acceptable; a slow redirect is not. The method catches and logs all exceptions silently.

**Expiry correctness over performance** - Redis eviction is always performed before the DB `UPDATE is_active = false`. If the app crashes between the two, the worst case is a cache miss that hits the DB and returns `410`. We never serve a redirect for a URL marked inactive.

**Rate limiting on creation only** - `POST /api/shorten` is rate limited at 5 requests per 60-second window per IP. Redirect traffic (`GET /{shortKey}`) is intentionally excluded — high redirect volume is expected behavior for a URL shortener.

**Fail-open rate limiting** - if Redis is unavailable, the rate limiter fails open (allows the request) rather than returning 429. Availability is preferred over strict enforcement when the enforcement mechanism itself is degraded.

---

## Running Locally

**Prerequisites:** Java 17, Maven, Docker

```bash
# Start Postgres
docker run -d --name url_shortener_postgres \
  -e POSTGRES_DB=url_shortener \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 postgres:15

# Start Redis
docker run -d --name my-redis-container -p 6379:6379 redis:7-alpine

# Run the app
./mvnw spring-boot:run
```

Configure application.properties :-
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener 
spring.datasource.username=postgres 
spring.datasource.password=your_password 
spring.data.redis.host=localhost 
spring.data.redis.port=6379  
```
