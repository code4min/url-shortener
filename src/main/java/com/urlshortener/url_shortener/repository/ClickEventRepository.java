package com.urlshortener.url_shortener.repository;

import com.urlshortener.url_shortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
	   
    long countByShortKey(String shortKey);
    
    long countByShortKeyAndClickedAtAfter(String shortKey, LocalDateTime after);
    
    @Query("""
        SELECT CAST(c.clickedAt AS DATE), COUNT(c)
        FROM ClickEvent c
        WHERE c.shortKey = :shortKey
        AND c.clickedAt >= :since
        GROUP BY CAST(c.clickedAt AS DATE)
        ORDER BY CAST(c.clickedAt AS DATE) DESC
    """)
    List<Object[]> getDailyClickCounts(
        @Param("shortKey") String shortKey,
        @Param("since") LocalDateTime since
    );

}
