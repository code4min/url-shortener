package com.urlshortener.url_shortener.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class ShortenUrlRequest {
	@NotBlank
	private String longUrl;
	
	private LocalDateTime expiresAt;

}
