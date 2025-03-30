package com.toyota.dtos.response;

public class ApiKeyResponse {
    private String ApiKey;

    public ApiKeyResponse(String apiKey) {
        ApiKey = apiKey;
    }


    public String getApiKey() {
        return ApiKey;
    }

    public void setApiKey(String apiKey) {
        ApiKey = apiKey;
    }


}
