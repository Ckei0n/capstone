package com.cap.stone.service;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OpenSearchClientService {
    
    @Autowired
    @Qualifier("customOpenSearchClient")
    private RestHighLevelClient client;
    
    public SearchResponse executeSearch(String indexPattern, SearchSourceBuilder sourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexPattern);
        searchRequest.source(sourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }
}