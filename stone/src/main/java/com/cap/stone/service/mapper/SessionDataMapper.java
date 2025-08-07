package com.cap.stone.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensearch.search.SearchHit;
import org.springframework.stereotype.Component;

@Component
public class SessionDataMapper {
    
    private static final String TIMESTAMP_FIELD = "@timestamp";
    
    public List<Map<String, Object>> mapSearchHitsToDataPoints(SearchHit[] hits) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (SearchHit hit : hits) {
            Map<String, Object> source = hit.getSourceAsMap();
            Map<String, Object> dataPoint = createDataPoint(source);
            results.add(dataPoint);
        }
        
        return results;
    }
    
    private Map<String, Object> createDataPoint(Map<String, Object> source) {
        Map<String, Object> dataPoint = new HashMap<>();
        
        dataPoint.put("timestamp", source.get(TIMESTAMP_FIELD));
        dataPoint.put("communityId", getNestedValue(source, "network", "community_id"));
        dataPoint.put("sid", getNestedValue(source, "extended", "sid"));
        dataPoint.put("session", source.get("session"));
        dataPoint.put("sourceIp", getNestedValue(source, "source", "ip"));
        dataPoint.put("destIp", getNestedValue(source, "destination", "ip"));
        dataPoint.put("sourcePort", getNestedValue(source, "source", "port"));
        dataPoint.put("destPort", getNestedValue(source, "destination", "port"));
        dataPoint.put("snortMessage", getNestedValue(source, "extended", "snort_message"));
        
        return dataPoint;
    }
    
    private Object getNestedValue(Map<String, Object> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (current instanceof Map<?, ?>) {
                Map<?, ?> currentMap = (Map<?, ?>) current;
                if (isStringObjectMap(currentMap)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedMap = (Map<String, Object>) currentMap;
                    current = typedMap.get(key);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }
    
    private boolean isStringObjectMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return true;
        }
        
        int checkCount = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                return false;
            }
            if (++checkCount >= 3) {
                break;
            }
        }
        return true;
    }
}
