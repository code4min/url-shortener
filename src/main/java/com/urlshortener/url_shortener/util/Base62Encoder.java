package com.urlshortener.url_shortener.util;

public class Base62Encoder {
	
    private static final String BASE62_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int BASE = 62;

    private Base62Encoder() {
        
    }

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }

        if (value == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        StringBuilder result = new StringBuilder();

        while (value > 0) {
            int remainder = (int) (value % BASE);
            result.append(BASE62_CHARS.charAt(remainder));
            value /= BASE;
        }

        return result.reverse().toString();
    }
}
