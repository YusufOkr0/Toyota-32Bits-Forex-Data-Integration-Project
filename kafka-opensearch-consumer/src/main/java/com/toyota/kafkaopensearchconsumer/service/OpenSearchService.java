package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;

/**
 * Service interface for indexing CurrencyPair documents into OpenSearch.
 */
public interface OpenSearchService {

    /**
     * Indexes the given {@link CurrencyPair} document into the specified OpenSearch index.
     *
     * @param currencyPair the object to be indexed
     * @param indexName the name of the OpenSearch index where the document will be stored
     * @throws RuntimeException if indexing fails due to any error
     */
    void indexCurrencyPair(CurrencyPair currencyPair, String indexName);
}
