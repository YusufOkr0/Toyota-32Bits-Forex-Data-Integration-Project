package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpenSearchServiceImpl {

    private final OpenSearchClient openSearchClient;

    /***
     * TODO: CHECK IF I NEED TO CREATE INDEX IN ADVANCE ??? IT CREATES INDEX BY DEFAULT IN CASE THERE IS NO INDEX.
     */
    public void indexCurrencyPair(CurrencyPair currencyPair, String indexName) {
        try {
            IndexRequest<CurrencyPair> request = new IndexRequest.Builder<CurrencyPair>()
                    .index(indexName)
                    .document(currencyPair)
                    .build();

            openSearchClient.index(request);

        } catch (Exception e) {
            throw new RuntimeException("Failed to index document to " + indexName, e);
        }
    }


}
