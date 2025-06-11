package com.openai.batchmanager.model;

public class BatchRequest {

    public enum RequestStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    private final String customId;
    private final String requestData;
    private String responseData;
    private RequestStatus status;

    public BatchRequest(String customId, String requestData) {
        if (customId == null || customId.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom ID cannot be null or empty");
        }
        if (requestData == null || requestData.trim().isEmpty()) {
            throw new IllegalArgumentException("Request data cannot be null or empty");
        }
        
        this.customId = customId.trim();
        this.requestData = requestData;
        this.status = RequestStatus.PENDING;
    }

    public String getCustomId() {
        return customId;
    }

    public String getRequestData() {
        return requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        this.status = status;
    }
}