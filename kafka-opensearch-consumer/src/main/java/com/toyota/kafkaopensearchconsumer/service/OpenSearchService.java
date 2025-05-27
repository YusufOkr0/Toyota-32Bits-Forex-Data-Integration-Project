package com.toyota.kafkaopensearchconsumer.service;

import com.toyota.kafkaopensearchconsumer.entity.CurrencyPair;

public interface OpenSearchService {
    void indexCurrencyPair(CurrencyPair currencyPair, String indexName);
}
