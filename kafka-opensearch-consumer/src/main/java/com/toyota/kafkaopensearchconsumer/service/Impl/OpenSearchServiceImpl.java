package com.toyota.kafkaopensearchconsumer.service.Impl;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;
import com.toyota.kafkaopensearchconsumer.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSearchServiceImpl implements OpenSearchService {

    private final OpenSearchClient openSearchClient;

    @Override
    public void indexCurrencyPair(CurrencyPair currencyPair, String indexName) {
        try {
            IndexRequest<CurrencyPair> request = new IndexRequest.Builder<CurrencyPair>()
                    .index(indexName)
                    .document(currencyPair)
                    .build();

            openSearchClient.index(request);
            log.info("Successfully indexed CurrencyPair into index '{}': {}", indexName, currencyPair);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index document to " + indexName, e);
        }
    }
}
