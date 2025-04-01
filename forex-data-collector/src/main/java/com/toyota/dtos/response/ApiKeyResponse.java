package com.toyota.dtos.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiKeyResponse {

    @JsonProperty("apiKey")
    private String apiKey;

    public ApiKeyResponse(String apiKey) {
        this.apiKey = apiKey;
    }
    public ApiKeyResponse() {
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}