package com.cap.stone.infra.opensearch.model;

import java.util.*;

//Represents aggregated stats across the entire date range
public class SessionAnalytics {
    private final List<Map<String, Object>> dailyData;
    private final int totalSnortHits;
    private final int totalUniqueSessions;

    //New SessionAnalytics object with computed stats.
    public SessionAnalytics(List<Map<String, Object>> dailyData, int totalSnortHits, 
                           int totalUniqueSessions) {
        this.dailyData = dailyData; // List of daily aggregated data points for timeseries visualization
        this.totalSnortHits = totalSnortHits; // Total count of Snort sids hits across the date range
        this.totalUniqueSessions = totalUniqueSessions; // Count of unique network sessions (community IDs) with sids
    }
    
    public List<Map<String, Object>> getDailyData() { return dailyData; }
    public int getTotalSnortHits() { return totalSnortHits; }
    public int getTotalUniqueSessions() { return totalUniqueSessions; }
    
     //Converts to the API response format expected by the controller.
    public Map<String, Object> toApiResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("timeseriesData", dailyData);
        response.put("totalSnortHits", totalSnortHits);
        response.put("totalUniqueSessions", totalUniqueSessions);
        return response;
    }
}