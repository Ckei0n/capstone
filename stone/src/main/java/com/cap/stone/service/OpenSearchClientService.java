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

// Allows for executing OpenSearch queries with common parameters.
@Service
public class OpenSearchClientService {
    
    @Autowired
    private OpenSearchClient client;
    
    // Executes a search query against OpenSearch with full parameter control
    public SearchResponse<Map<String, Object>> executeSearch(String indexPattern, Query query, Integer size, String[] sourceFields, String sortField, SortOrder sortOrder) throws IOException {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(indexPattern)
            .query(query);
        
        // Apply size limit if specified
        if (size != null) {
            searchBuilder.size(size);
        }
        
        // // Apply field filtering if specified (like a SELECT clause in SQL)
        if (sourceFields != null) {
            searchBuilder.source(s -> s.filter(f -> f.includes(java.util.Arrays.asList(sourceFields))));
        }
        
        // Apply sorting if specified
        if (sortField != null && sortOrder != null) {
            searchBuilder.sort(sort -> sort.field(f -> f.field(sortField).order(sortOrder)));
        }
        
        //casting due to type erasure
        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
        
        return client.search(searchBuilder.build(), mapClass);
    }
}