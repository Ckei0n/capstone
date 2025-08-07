package com.cap.stone.config;

import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class OpenSearchConfig extends AbstractOpenSearchConfiguration {

    @Override
    @Bean(name = "customOpenSearchClient") //use because custom bean name becausee opensearchClient shares the same bean name
    public RestHighLevelClient opensearchClient() {

        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("opensearch-node1:9200")
              //  .usingSsl()  // enable HTTPS
              //  .withBasicAuth()  // add your credentials
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
}
