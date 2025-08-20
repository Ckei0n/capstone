package com.cap.stone.controller;

import com.cap.stone.infra.opensearch.SessionAnalyticsService;
import com.cap.stone.infra.opensearch.model.SessionAnalytics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// REST API endpoints for querying network session data
@RestController
@RequestMapping("/api")
public class SessionController {
    
    @Autowired
    private SessionAnalyticsService sessionAnalyticsService;
    
    // Retrieves network sessions for a specified date range.
    @GetMapping("/sessions")
    public Object getSessions(@RequestParam String start,
                             @RequestParam String end) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Parse and validate dates
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            
            // validation for dates
            if (startDate.isAfter(endDate)) {
                response.put("error", "Start date cannot be after end date");
                return response;
            }
            
            // Prevent large queries that could impact performance
            if (startDate.plusYears(1).isBefore(endDate)) {
                response.put("error", "Date range too large (max 1 year)");
                return response;
            }
            
            SessionAnalytics analytics = sessionAnalyticsService.getSessionAnalytics(start, end);
            
            return analytics.toApiResponse();
            
        } catch (DateTimeParseException e) {
            response.put("error", "Invalid date format. Use YYYY-MM-DD");
        } catch (IOException e) {
            response.put("error", "Error fetching data");
        }
        return response;
    }

    
    //Retrieves all session data for a specific day.
    @GetMapping("/sessions/daily-details")
    public Object getDailySessionDetails(@RequestParam String start,
                                       @RequestParam String end,
                                       @RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Parse and validate dates
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            LocalDate targetDate = LocalDate.parse(date);
            
            // Validate that target date is within the specified range
            if (targetDate.isBefore(startDate) || targetDate.isAfter(endDate)) {
                response.put("error", "Target date must be within the specified date range");
                return response;
            }
            
            // Get session details for the specific day
            List<Map<String, Object>> sessions = sessionAnalyticsService.getSessionsForSpecificDay(date);
            
            response.put("sessions", sessions);
            response.put("date", date);
            response.put("totalSessions", sessions.size());
            
            return response;
            
        } catch (DateTimeParseException e) {
            response.put("error", "Invalid date format. Use YYYY-MM-DD");
        } catch (IOException e) {
            response.put("error", "Error fetching session details");
        }
        return response;
    }
}