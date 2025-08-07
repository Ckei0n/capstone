package com.cap.stone.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cap.stone.infra.opensearch.OpenSearchClientWrapper;


import java.util.List;
import java.util.Map;

@Service
public class OsSearchService {

    
    @Autowired
    private OpenSearchClientWrapper clientWrapper;
    
    public int countSnortHits(String startDate, String endDate) throws IOException {
        return clientWrapper.countSnortHitsByDateRange(startDate, endDate);
    }
    
    public int countUniqueCommunityIds(String startDate, String endDate) throws IOException {
        return clientWrapper.countUniqueCommunityIdsByDateRange(startDate, endDate);
    }
    
    public Map<String, Object> getDailyTimeseriesData(String startDate, String endDate) throws IOException {
        return clientWrapper.executeDailyAggregationQueryByDateRange(startDate, endDate);
    }
    
    public List<Map<String, Object>> getSessionsForSpecificDay(String startDate, String endDate, String date) throws IOException {
        return clientWrapper.getSessionsForSpecificDay(date);
    }
}