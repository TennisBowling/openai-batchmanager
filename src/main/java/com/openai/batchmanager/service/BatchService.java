package com.openai.batchmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.batchmanager.client.OpenAIClient;
import com.openai.batchmanager.db.DatabaseManager;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchRequest;
import com.openai.batchmanager.util.JsonUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchService {

    private final DatabaseManager db;
    private final OpenAIClient client;
    private final ObjectMapper mapper;

    public BatchService(DatabaseManager db, OpenAIClient client) {
        this.db = db;
        this.client = client;
        this.mapper = JsonUtils.getMapper();
    }

    public void submitBatch(Batch batch, List<BatchRequest> requests)
            throws SQLException, IOException {

        batch.setTotalRequests(requests.size());
        batch.setCompletedRequests(0);
        db.insertBatch(batch);
        for (BatchRequest r : requests) {
            db.insertBatchRequest(batch.getId(), r);
        }

        Map<String, String> customIdToRequestJson = new HashMap<>();
        for (BatchRequest r : requests) {
            customIdToRequestJson.put(r.getCustomId(), r.getRequestData());
        }

        String endpoint = determineEndpoint(requests.get(0).getRequestData());
        
        Batch submittedBatch = client.submitBatch(customIdToRequestJson, endpoint);

        updateBatchFromSubmitted(batch, submittedBatch);
        db.updateBatch(batch);
    }

    public void submitBatchWithMetadata(Batch batch, List<BatchRequest> requests, Map<String, String> metadata)
            throws SQLException, IOException {

        batch.setTotalRequests(requests.size());
        batch.setCompletedRequests(0);
        batch.setMetadata(metadata);
        db.insertBatch(batch);
        for (BatchRequest r : requests) {
            db.insertBatchRequest(batch.getId(), r);
        }

        Map<String, String> customIdToRequestJson = new HashMap<>();
        for (BatchRequest r : requests) {
            customIdToRequestJson.put(r.getCustomId(), r.getRequestData());
        }

        String endpoint = determineEndpoint(requests.get(0).getRequestData());
        
        Batch submittedBatch = client.submitBatchWithMetadata(customIdToRequestJson, endpoint, metadata);

        updateBatchFromSubmitted(batch, submittedBatch);
        db.updateBatch(batch);
    }

    public boolean pollBatchStatus(Batch batch) throws IOException, SQLException {
        if (batch.getStatus().isTerminal()) {
            return true;
        }

        Batch updatedBatch = client.updateBatchStatus(batch);
        
        updateBatchFromPolled(batch, updatedBatch);
        db.updateBatch(batch);
        
        return batch.getStatus().isTerminal();
    }

    public Map<String, String> fetchResults(Batch batch, List<BatchRequest> requests)
            throws IOException, SQLException {

        Map<String, String> openaiResults = client.downloadBatchResults(batch);
        Map<String, String> mapped = new HashMap<>();

        int completed = 0;
        for (BatchRequest req : requests) {
            String responseJson = openaiResults.get(req.getCustomId());
            if (responseJson != null) {
                req.setResponseData(responseJson);
                req.setStatus(BatchRequest.RequestStatus.COMPLETED);
                db.updateBatchRequest(batch.getId(), req);
                mapped.put(req.getCustomId(), responseJson);
                completed++;
            }
        }
        batch.setCompletedRequests(completed);
        db.updateBatch(batch);
        return mapped;
    }

    public void cancelBatch(Batch batch) throws IOException, SQLException {
        Batch cancelledBatch = client.cancelBatch(batch.getOpenaiBatchId());
        updateBatchFromPolled(batch, cancelledBatch);
        db.updateBatch(batch);
    }

    public String listBatches(Integer limit, String after) throws IOException {
        return client.listBatches(limit, after);
    }

    public List<Batch> getIncompleteBatches() throws SQLException {
        return db.getIncompleteBatches();
    }

    public List<BatchRequest> getBatchRequests(String batchId) throws SQLException {
        return db.getBatchRequests(batchId);
    }

    private String determineEndpoint(String requestJson) {
        try {
            JsonNode json = mapper.readTree(requestJson);
            
            // Check for required model field
            if (!json.has("model")) {
                throw new IllegalArgumentException("Request JSON must contain a 'model' field");
            }
            
            // Determine endpoint based on request structure
            if (json.has("messages")) {
                return "/v1/chat/completions";
            } else if (json.has("input")) {
                return "/v1/embeddings";
            } else if (json.has("prompt")) {
                return "/v1/completions";
            } else if (json.has("instruction") || json.has("file")) {
                return "/v1/fine_tuning/jobs";
            } else {
                // Default fallback - most common endpoint
                return "/v1/chat/completions";
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON in request data: " + e.getMessage(), e);
        }
    }

    private void updateBatchFromSubmitted(Batch localBatch, Batch submittedBatch) {
        localBatch.setOpenaiBatchId(submittedBatch.getOpenaiBatchId());
        localBatch.setStatus(submittedBatch.getStatus());
        localBatch.setEndpoint(submittedBatch.getEndpoint());
        localBatch.setInputFileId(submittedBatch.getInputFileId());
        localBatch.setCompletionWindow(submittedBatch.getCompletionWindow());
        localBatch.setExpiresAt(submittedBatch.getExpiresAt());
        localBatch.setRequestCounts(submittedBatch.getRequestCounts());
        if (submittedBatch.getMetadata() != null) {
            localBatch.setMetadata(submittedBatch.getMetadata());
        }
        if (submittedBatch.getSubmittedAt() != null) {
            localBatch.setSubmittedAt(submittedBatch.getSubmittedAt());
        }
    }

    private void updateBatchFromPolled(Batch localBatch, Batch polledBatch) {
        localBatch.setStatus(polledBatch.getStatus());
        localBatch.setOutputFileId(polledBatch.getOutputFileId());
        localBatch.setErrorFileId(polledBatch.getErrorFileId());
        localBatch.setRequestCounts(polledBatch.getRequestCounts());
        
        if (polledBatch.getSubmittedAt() != null) {
            localBatch.setSubmittedAt(polledBatch.getSubmittedAt());
        }
        if (polledBatch.getCompletedAt() != null) {
            localBatch.setCompletedAt(polledBatch.getCompletedAt());
        }
        if (polledBatch.getFailedAt() != null) {
            localBatch.setFailedAt(polledBatch.getFailedAt());
        }
        if (polledBatch.getExpiredAt() != null) {
            localBatch.setExpiredAt(polledBatch.getExpiredAt());
        }
        if (polledBatch.getFinalizingAt() != null) {
            localBatch.setFinalizingAt(polledBatch.getFinalizingAt());
        }
        if (polledBatch.getCancellingAt() != null) {
            localBatch.setCancellingAt(polledBatch.getCancellingAt());
        }
        if (polledBatch.getCancelledAt() != null) {
            localBatch.setCancelledAt(polledBatch.getCancelledAt());
        }
    }
}