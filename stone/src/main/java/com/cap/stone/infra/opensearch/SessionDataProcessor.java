package com.cap.stone.infra.opensearch;

import org.opensearch.search.SearchHit;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SessionDataProcessor {
    
    private static final String TIMESTAMP_FIELD = "@timestamp";
    
    public Map<String, Object> processHit(SearchHit hit) {
        Map<String, Object> session = hit.getSourceAsMap();
        session.put("_index", hit.getIndex());
        session.put("_id", hit.getId());
        
        Map<String, Object> processed = new HashMap<>();
        processed.put("timestamp", session.get(TIMESTAMP_FIELD));
        processed.put("indexName", session.get("_index"));
        processed.put("documentId", session.get("_id"));
        processed.put("communityId", getNestedValue(session, "network", "community_id"));
        processed.put("sid", getNestedValue(session, "extended", "sid"));
        processed.put("session", session.get("session"));
        processed.put("sourceIp", getNestedValue(session, "source", "ip"));
        processed.put("destIp", getNestedValue(session, "destination", "ip"));
        processed.put("sourcePort", getNestedValue(session, "source", "port"));
        processed.put("destPort", getNestedValue(session, "destination", "port"));
        processed.put("snortMessage", getNestedValue(session, "extended", "snort_message"));
        
        return processed;
    }
    
    public List<Long> extractSids(Object sidObj) {
        if (sidObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> sidList = (List<Object>) sidObj;
            return sidList.stream()
                .filter(s -> s instanceof Number)
                .map(s -> ((Number) s).longValue())
                .collect(Collectors.toList());
        } else if (sidObj instanceof Number) {
            return List.of(((Number) sidObj).longValue());
        }
        return new ArrayList<>();
    }
    
    public String[] getDetailedFields() {
        return new String[]{
            TIMESTAMP_FIELD,
            "network.community_id",
            "extended.sid",
            "session",
            "source.ip",
            "destination.ip",
            "source.port",
            "destination.port",
            "extended.snort_message"
        };
    }
    
    private Object getNestedValue(Map<String, Object> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (current instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
            } else {
                return null;
            }
        }
        return current;
    }
}
