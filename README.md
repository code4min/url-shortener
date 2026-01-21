# 🔗 URL Shortener

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![License](https://img.shields.io/github/license/<code4min>/url-shortener)

A **backend-first URL shortener** built with **Java, Spring Boot, and PostgreSQL**, designed with clean architecture and scalability in mind.

## Features (v1)

- URL shortening using **Base62 encoding**
- Collision-free short key generation
- Persistent storage with PostgreSQL
- Fast **302 redirects**
- Optional URL expiry
- Click count tracking
- Global exception handling
- Clean layered architecture

## Architecture

Controller → Service → Repository → Database

- Stateless encoding logic
- Transaction-safe operations
- Production-style error handling

## API Endpoints

### Create short URL
POST /api/shorten

**Request**
json
{
  "longUrl": "https://www.somewebsitename.com",
  "expiresAt": null
}
**Response**
{
  "shortUrl": "http://localhost:8080/b"
}
**Redirect**
GET /{shortKey}

## Tech Stack 

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Maven

## Status

-> v1.0 released
-> v2 in progress (adaptive redirects, caching, analytics)

## License

This project is licensed under the MIT License.

## Developer

https://github.com/code4min/

