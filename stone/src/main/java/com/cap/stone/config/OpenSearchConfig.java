package com.cap.stone.config;

import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Bean
    public OpenSearchClient openSearchClient() {
        // Define your OpenSearch HTTP host
        final HttpHost host = new HttpHost("http", "opensearch-node1", 9200);

        // Build the Apache HTTP client
        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(host);
        builder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
            .setConnectionManager(PoolingAsyncClientConnectionManagerBuilder.create().build())
        );

        // Create and return the OpenSearch client
        OpenSearchTransport transport = builder.build();
        return new OpenSearchClient(transport);
    }
}
