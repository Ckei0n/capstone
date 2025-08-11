package com.cap.stone.infra.opensearch.model;

import java.util.*;
import java.util.stream.Collectors;

public class SessionGroup {
    private final String date;
    private final List<Map<String, Object>> sessions = new ArrayList<>();
    private final List<String> allCommunityIds = new ArrayList<>();
    private final List<Long> allSids = new ArrayList<>();
    
    public SessionGroup(String date) {
        this.date = date;
    }
    
    public void addSession(Map<String, Object> session) {
        sessions.add(session);
    }
    
    public void addCommunityIds(List<String> communityIds) {
        allCommunityIds.addAll(communityIds);
    }
    
    public void addSids(List<Long> sids) {
        allSids.addAll(sids);
    }
    
    public String getDate() {
        return date;
    }
    
    public int getSessionCount() {
        return sessions.size();
    }
    
    public List<Map<String, Object>> getSampleSessions(int limit) {
        return sessions.subList(0, Math.min(limit, sessions.size()));
    }
    
    public boolean hasMoreSessionsThan(int threshold) {
        return sessions.size() > threshold;
    }
    
    public List<String> getUniqueCommunityIds() {
        return allCommunityIds.stream().distinct().collect(Collectors.toList());
    }
    
    public List<Long> getUniqueSids() {
        return allSids.stream().distinct().collect(Collectors.toList());
    }
    
    public Map<String, Integer> getCommunityIdHitCounts() {
        Map<String, Integer> counts = new HashMap<>();
        
        for (Map<String, Object> session : sessions) {
            String communityId = (String) session.get("communityId");
            if (communityId != null) {
                counts.put(communityId, counts.getOrDefault(communityId, 0) + 1);
            }
        }
        
        return counts;
    }
}