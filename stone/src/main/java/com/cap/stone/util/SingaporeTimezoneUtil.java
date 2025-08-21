package com.cap.stone.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

// handles conversions between UTC and Singapore time (SGT).
@Component
public class SingaporeTimezoneUtil {
    
    private static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");
    private static final DateTimeFormatter INDEX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    
    // Converts UTC timestamp to Singapore time timestamp
    public long convertToSingaporeTimestamp(Object timestampObj) {
        try {
            Instant utcInstant = parseTimestampToInstant(timestampObj);
            
            // Convert to Singapore time and return as epoch milliseconds
            ZonedDateTime singaporeTime = utcInstant.atZone(SINGAPORE_ZONE);
            return singaporeTime.toInstant().toEpochMilli();
            
        } catch (Exception e) {
            System.err.println("Error converting timestamp to Singapore time: " + timestampObj + " - " + e.getMessage());
            // Return current time in Singapore as fallback
            return ZonedDateTime.now(SINGAPORE_ZONE).toInstant().toEpochMilli();
        }
    }
    
     // Parses various timestamp formats to Instant
    private Instant parseTimestampToInstant(Object timestampObj) {
        if (timestampObj instanceof String) {
            // Parse ISO string timestamp
            return Instant.parse((String) timestampObj);
        } else if (timestampObj instanceof Long) {
            // Handle epoch milliseconds
            return Instant.ofEpochMilli((Long) timestampObj);
        } else if (timestampObj instanceof Number) {
            // Handle other numeric types
            return Instant.ofEpochMilli(((Number) timestampObj).longValue());
        } else {
            // try to parse as string
            return Instant.parse(timestampObj.toString());
        }
    }
    
    // Gets the Singapore date (YYYY-MM-DD) from a Singapore timestamp
    public String getSingaporeDateFromTimestamp(long singaporeTimestamp) {
        Instant instant = Instant.ofEpochMilli(singaporeTimestamp);
        ZonedDateTime singaporeDateTime = instant.atZone(SINGAPORE_ZONE);
        return singaporeDateTime.toLocalDate().toString();
    }
    
    // Builds OpenSearch index pattern for a specific date, creates index patterns like "arkime_sessions*-250825" for 2025-08-25
    public String buildIndexPattern(LocalDate date) {
        String indexSuffix = date.format(INDEX_DATE_FORMATTER);
        return "arkime_sessions*-" + indexSuffix;
    }
    
    // Gets index patterns for timezone boundary queries, need to check multiple index patterns because of timezone boundaries

    public List<String> getIndexPatternsForSingaporeDate(LocalDate targetDate) {

        // Query current day, previous day, and next day indices to handle timezone boundaries
        return Arrays.asList(
            buildIndexPattern(targetDate.minusDays(1)),  // Previous day (for early morning SGT)
            buildIndexPattern(targetDate),               // Target day
            buildIndexPattern(targetDate.plusDays(1))    // Next day (for late night SGT)
        );
    }
    
    // Gets index patterns for date range, check current and next day indices to catch timezone boundary cases.
    public List<String> getIndexPatternsForAnalytics(LocalDate currentDate) {
        return Arrays.asList(
            buildIndexPattern(currentDate.minusDays(1)),  // Previous day
            buildIndexPattern(currentDate),              // Current day
            buildIndexPattern(currentDate.plusDays(1))   // Next day
        );
    }
    
    // Used for D3.js chart positioning, creates a timestamp representing the start of a day in Singapore timezone.
    public long getSingaporeStartOfDayTimestamp(LocalDate date) {
        ZonedDateTime singaporeStartOfDay = date.atStartOfDay(SINGAPORE_ZONE);
        return singaporeStartOfDay.toInstant().toEpochMilli();
    }
    
    // Check if a converted Singapore timestamp falls on the target Singapore date.
    public boolean timestampBelongsToSingaporeDate(long singaporeTimestamp, String targetSingaporeDate) {
        String actualSingaporeDate = getSingaporeDateFromTimestamp(singaporeTimestamp);
        return actualSingaporeDate.equals(targetSingaporeDate);
    }
}