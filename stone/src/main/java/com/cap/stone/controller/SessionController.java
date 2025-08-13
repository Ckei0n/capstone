package com.cap.stone.controller;

import com.cap.stone.infra.opensearch.SessionDataService;
import com.cap.stone.infra.opensearch.SessionQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SessionController {
    
    @Autowired
    private SessionQueryService sessionQueryService;
    
    @Autowired
    private SessionDataService sessionDataService;
    
    @GetMapping("/sessions")
    public Object getSessions(@RequestParam String start,
                             @RequestParam String end) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            
            if (startDate.isAfter(endDate)) {
                response.put("error", "Start date cannot be after end date");
                return response;
            }
            
            if (startDate.plusYears(1).isBefore(endDate)) {
                response.put("error", "Date range too large (max 1 year)");
                return response;
            }
            
            int totalUniqueSessions = sessionQueryService.countUniqueCommunityIdsByDateRange(start, end);
            response.put("totalUniqueSessions", totalUniqueSessions);
            
            int snortHits = sessionQueryService.countSnortHitsByDateRange(start, end);
            response.put("snortHits", snortHits);
            
            Map<String, Object> timeseriesResult = sessionDataService.getDailyTimeseriesData(start, end);
            response.put("timeseriesData", timeseriesResult.get("dailyData"));
            response.put("totalHitsInRange", timeseriesResult.get("totalHits"));
            
        } catch (DateTimeParseException e) {
            response.put("error", "Invalid date format. Use YYYY-MM-DD");
        } catch (IOException e) {
            response.put("error", "Error fetching data");
        }
        return response;
    }
}