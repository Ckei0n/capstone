package com.cap.stone.service;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class OpenSearchClientService {
    
    @Autowired
    private OpenSearchClient client;
    
    public SearchResponse<Map<String, Object>> executeSearch(String indexPattern, Query query, Integer size, String[] sourceFields, String sortField, SortOrder sortOrder) throws IOException {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(indexPattern)
            .query(query);
            
        if (size != null) {
            searchBuilder.size(size);
        }
        
        if (sourceFields != null) {
            searchBuilder.source(s -> s.filter(f -> f.includes(java.util.Arrays.asList(sourceFields))));
        }
        
        if (sortField != null && sortOrder != null) {
            searchBuilder.sort(sort -> sort.field(f -> f.field(sortField).order(sortOrder)));
        }
        
        
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
        
        return client.search(searchBuilder.build(), mapClass);
    }
    
    public SearchResponse<Map<String, Object>> executeSearch(String indexPattern, Query query, Integer size, String[] sourceFields) throws IOException {
        return executeSearch(indexPattern, query, size, sourceFields, null, null);
    }
    
    public SearchResponse<Map<String, Object>> executeSearch(String indexPattern, Query query) throws IOException {
        return executeSearch(indexPattern, query, null, null, null, null);
    }
}