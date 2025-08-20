package com.cap.stone.infra.opensearch.model;

import java.util.*;
import java.util.stream.Collectors;

// Represents data for a single day
public class SessionGroup {
    private final String date; // Date this group represents
    private final List<Map<String, Object>> sessions = new ArrayList<>(); // All sessions for this date
    private final List<String> allCommunityIds = new ArrayList<>(); // All community IDs (with duplicates)
    private final List<Long> allSids = new ArrayList<>(); // All Snort SIDs (with duplicates)
    
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
    
    //Returns a limited sample of sessions for display purposes.
    public List<Map<String, Object>> getSampleSessions(int limit) {
        return sessions.subList(0, Math.min(limit, sessions.size()));
    }
    
    //Checks if this group has more sessions than the specified threshold. Used for pagination indicators in the UI
    public boolean hasMoreSessionsThan(int threshold) {
        return sessions.size() > threshold;
    }
    
    public List<String> getUniqueCommunityIds() {
        return allCommunityIds.stream().distinct().collect(Collectors.toList());
    }
    
    public List<Long> getUniqueSids() {
        return allSids.stream().distinct().collect(Collectors.toList());
    }

    //Calculates hit counts per community ID for this date
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