package com.openai.batchmanager.model;

import java.time.Instant;
import java.util.Map;

public class Batch {

    private final String id;
    private String openaiBatchId;
    private BatchStatus status;
    private final Instant createdAt;
    
    private String endpoint;
    private String inputFileId;
    private String outputFileId;
    private String errorFileId;
    private String completionWindow;
    private Map<String, String> metadata;
    private RequestCounts requestCounts;
    
    private Instant submittedAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant expiredAt;
    private Instant expiresAt;
    private Instant finalizingAt;
    private Instant cancellingAt;
    private Instant cancelledAt;
    
    private int totalRequests;
    private int completedRequests;

    public Batch(String id) {
        this.id = id;
        this.status = BatchStatus.PENDING;
        this.createdAt = Instant.now();
        this.completionWindow = "24h"; // Default completion window
        this.requestCounts = new RequestCounts();
    }

    public String getId() {
        return id;
    }

    public String getOpenaiBatchId() {
        return openaiBatchId;
    }

    public void setOpenaiBatchId(String openaiBatchId) {
        this.openaiBatchId = openaiBatchId;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getInputFileId() {
        return inputFileId;
    }

    public void setInputFileId(String inputFileId) {
        this.inputFileId = inputFileId;
    }

    public String getOutputFileId() {
        return outputFileId;
    }

    public void setOutputFileId(String outputFileId) {
        this.outputFileId = outputFileId;
    }

    public String getErrorFileId() {
        return errorFileId;
    }

    public void setErrorFileId(String errorFileId) {
        this.errorFileId = errorFileId;
    }

    public String getCompletionWindow() {
        return completionWindow;
    }

    public void setCompletionWindow(String completionWindow) {
        this.completionWindow = completionWindow;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public RequestCounts getRequestCounts() {
        return requestCounts;
    }

    public void setRequestCounts(RequestCounts requestCounts) {
        this.requestCounts = requestCounts;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getFinalizingAt() {
        return finalizingAt;
    }

    public void setFinalizingAt(Instant finalizingAt) {
        this.finalizingAt = finalizingAt;
    }

    public Instant getCancellingAt() {
        return cancellingAt;
    }

    public void setCancellingAt(Instant cancellingAt) {
        this.cancellingAt = cancellingAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    // Legacy getters/setters for backward compatibility
    public int getTotalRequests() {
        return requestCounts != null ? requestCounts.getTotal() : totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
        if (requestCounts != null) {
            requestCounts.setTotal(totalRequests);
        }
    }

    public int getCompletedRequests() {
        return requestCounts != null ? requestCounts.getCompleted() : completedRequests;
    }

    public void setCompletedRequests(int completedRequests) {
        this.completedRequests = completedRequests;
        if (requestCounts != null) {
            requestCounts.setCompleted(completedRequests);
        }
    }

    @Override
    public String toString() {
        return "Batch{" +
                "id='" + id + '\'' +
                ", openaiBatchId='" + openaiBatchId + '\'' +
                ", status=" + status +
                ", endpoint='" + endpoint + '\'' +
                ", createdAt=" + createdAt +
                ", submittedAt=" + submittedAt +
                ", completedAt=" + completedAt +
                ", requestCounts=" + requestCounts +
                '}';
    }
}