package com.cap.stone.config;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    // Creates and configures an OpenSearch client bean for connecting to the OpenSearch cluster.
    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            final HttpHost httpHost = new HttpHost("https", "opensearch-node1", 9200);
            
            // Configure basic authentication credentials
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                new AuthScope(httpHost), 
                new UsernamePasswordCredentials("admin", "MyStrongPassword123!".toCharArray())
            );

            // Create SSL context that trusts all certificates (for demo certificates)
            final SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(null, (chains, authType) -> true)
                .build();

            // Build the HTTP transport layer
            final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(httpHost);
            
            // Configure HTTP client with SSL and credentials
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    // Create connection manager with SSL context
                    final AsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                        .create()
                        .setTlsStrategy(ClientTlsStrategyBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier((hostname, session) -> true) // Disable hostname verification
                            .buildAsync())
                        .build();

                    // Apply credentials and connection manager to HTTP client
                    httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setConnectionManager(connectionManager);

                return httpClientBuilder;
            });

            // Set timeouts
            builder.setRequestConfigCallback(requestConfigBuilder -> 
                requestConfigBuilder
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(30000))
                    .setResponseTimeout(Timeout.ofMilliseconds(60000))
            );

            // Build final transport and create OpenSearch client
            final OpenSearchTransport transport = builder.build();
            return new OpenSearchClient(transport);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create OpenSearch client", e);
        }
    }
}
