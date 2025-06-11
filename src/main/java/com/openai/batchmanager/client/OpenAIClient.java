package com.openai.batchmanager.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchRequestOutput;
import com.openai.batchmanager.model.BatchStatus;
import com.openai.batchmanager.model.RequestCounts;
import com.openai.batchmanager.util.JsonUtils;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class OpenAIClient {

    private static final String BASE_URL = "https://api.openai.com/v1/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType JSONL = MediaType.get("application/jsonl");

    private final String apiKey;
    private final OkHttpClient http;
    private final ObjectMapper mapper;

    public OpenAIClient(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofMinutes(5))
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(2))
                .writeTimeout(Duration.ofMinutes(2))
                .build();
        this.mapper = JsonUtils.getMapper();
    }

    public Batch submitBatch(Map<String, String> customIdToRequestJson, String endpoint) throws IOException {
        if (customIdToRequestJson == null || customIdToRequestJson.isEmpty()) {
            throw new IllegalArgumentException("customIdToRequestJson cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        }
        String jsonlContent = JsonUtils.createBatchInputJsonlWithCustomIds(customIdToRequestJson, endpoint);
        
        String fileId = uploadFile(jsonlContent, "batch");
        
        String batchJson = createBatch(fileId, endpoint, "24h", null);
        
        return parseBatchFromJson(batchJson);
    }

    public Batch submitBatchWithMetadata(Map<String, String> customIdToRequestJson, String endpoint, Map<String, String> metadata) throws IOException {
        if (customIdToRequestJson == null || customIdToRequestJson.isEmpty()) {
            throw new IllegalArgumentException("customIdToRequestJson cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        }
        String jsonlContent = JsonUtils.createBatchInputJsonlWithCustomIds(customIdToRequestJson, endpoint);
        
        String fileId = uploadFile(jsonlContent, "batch");
        
        String batchJson = createBatch(fileId, endpoint, "24h", metadata);
        
        return parseBatchFromJson(batchJson);
    }

    public Batch updateBatchStatus(Batch batch) throws IOException {
        if (batch == null) {
            throw new IllegalArgumentException("batch cannot be null");
        }
        if (batch.getOpenaiBatchId() == null || batch.getOpenaiBatchId().trim().isEmpty()) {
            throw new IllegalArgumentException("batch must have a valid OpenAI batch ID");
        }
        String batchJson = getBatch(batch.getOpenaiBatchId());
        return updateBatchFromJson(batch, batchJson);
    }

    public Map<String, String> downloadBatchResults(Batch batch) throws IOException {
        if (batch == null) {
            throw new IllegalArgumentException("batch cannot be null");
        }
        if (batch.getOutputFileId() == null) {
            throw new IllegalStateException("Batch has no output file ID");
        }
        
        String jsonlContent = downloadFile(batch.getOutputFileId());
        List<BatchRequestOutput> outputs = JsonUtils.parseBatchOutputJsonl(jsonlContent);
        return JsonUtils.outputsToResponseMap(outputs);
    }

    public Batch cancelBatch(String batchId) throws IOException {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("batchId cannot be null or empty");
        }
        String batchJson = cancelBatchRequest(batchId);
        return parseBatchFromJson(batchJson);
    }

    public String listBatches(Integer limit, String after) throws IOException {
        return listBatchesRequest(limit, after);
    }

    private String uploadFile(String content, String purpose) throws IOException {
        RequestBody fileBody = RequestBody.create(content, JSONL);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "batch_input.jsonl", fileBody)
                .addFormDataPart("purpose", purpose)
                .build();

        Request request = authorisedRequest(BASE_URL + "files")
                .post(requestBody)
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to upload file: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Upload response body is null");
            }
            String responseJson = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseJson);
            return JsonUtils.getStringValue(jsonNode, "id");
        }
    }

    private String createBatch(String inputFileId, String endpoint, String completionWindow, Map<String, String> metadata) throws IOException {
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("input_file_id", inputFileId);
        requestBody.put("endpoint", endpoint);
        requestBody.put("completion_window", completionWindow);
        
        if (metadata != null && !metadata.isEmpty()) {
            ObjectNode metadataNode = mapper.createObjectNode();
            metadata.forEach(metadataNode::put);
            requestBody.set("metadata", metadataNode);
        }

        Request request = authorisedRequest(BASE_URL + "batches")
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to create batch: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Create batch response body is null");
            }
            return response.body().string();
        }
    }

    private String getBatch(String batchId) throws IOException {
        Request request = authorisedRequest(BASE_URL + "batches/" + batchId)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to get batch: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Get batch response body is null");
            }
            return response.body().string();
        }
    }

    private String cancelBatchRequest(String batchId) throws IOException {
        Request request = authorisedRequest(BASE_URL + "batches/" + batchId + "/cancel")
                .post(RequestBody.create("", JSON))
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to cancel batch: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Cancel batch response body is null");
            }
            return response.body().string();
        }
    }

    private String listBatchesRequest(Integer limit, String after) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "batches").newBuilder();
        
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        }
        if (after != null) {
            urlBuilder.addQueryParameter("after", after);
        }

        Request request = authorisedRequest(urlBuilder.build().toString())
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to list batches: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("List batches response body is null");
            }
            return response.body().string();
        }
    }

    private String downloadFile(String fileId) throws IOException {
        Request request = authorisedRequest(BASE_URL + "files/" + fileId + "/content")
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "null";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                throw new IOException("Failed to download file: " + response.code() + " " + response.message() +
                                    "\nResponse: " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Download file response body is null");
            }
            return response.body().string();
        }
    }

    private Batch parseBatchFromJson(String jsonResponse) throws IOException {
        JsonNode json = mapper.readTree(jsonResponse);
        
        Batch batch = new Batch(JsonUtils.getStringValue(json, "id"));
        updateBatchFromJsonNode(batch, json);
        
        return batch;
    }

    private Batch updateBatchFromJson(Batch batch, String jsonResponse) throws IOException {
        JsonNode json = mapper.readTree(jsonResponse);
        updateBatchFromJsonNode(batch, json);
        return batch;
    }

    private void updateBatchFromJsonNode(Batch batch, JsonNode json) {
        batch.setOpenaiBatchId(JsonUtils.getStringValue(json, "id"));
        
        String statusStr = JsonUtils.getStringValue(json, "status");
        if (statusStr != null) {
            batch.setStatus(BatchStatus.fromOpenAIStatus(statusStr));
        }
        
        batch.setEndpoint(JsonUtils.getStringValue(json, "endpoint"));
        batch.setInputFileId(JsonUtils.getStringValue(json, "input_file_id"));
        batch.setOutputFileId(JsonUtils.getStringValue(json, "output_file_id"));
        batch.setErrorFileId(JsonUtils.getStringValue(json, "error_file_id"));
        batch.setCompletionWindow(JsonUtils.getStringValue(json, "completion_window"));
        
        long inProgressAt = JsonUtils.getLongValue(json, "in_progress_at", 0);
        long completedAt = JsonUtils.getLongValue(json, "completed_at", 0);
        long failedAt = JsonUtils.getLongValue(json, "failed_at", 0);
        long expiredAt = JsonUtils.getLongValue(json, "expired_at", 0);
        long expiresAt = JsonUtils.getLongValue(json, "expires_at", 0);
        long finalizingAt = JsonUtils.getLongValue(json, "finalizing_at", 0);
        long cancellingAt = JsonUtils.getLongValue(json, "cancelling_at", 0);
        long cancelledAt = JsonUtils.getLongValue(json, "cancelled_at", 0);
        
        if (inProgressAt > 0) batch.setSubmittedAt(Instant.ofEpochSecond(inProgressAt));
        if (completedAt > 0) batch.setCompletedAt(Instant.ofEpochSecond(completedAt));
        if (failedAt > 0) batch.setFailedAt(Instant.ofEpochSecond(failedAt));
        if (expiredAt > 0) batch.setExpiredAt(Instant.ofEpochSecond(expiredAt));
        if (expiresAt > 0) batch.setExpiresAt(Instant.ofEpochSecond(expiresAt));
        if (finalizingAt > 0) batch.setFinalizingAt(Instant.ofEpochSecond(finalizingAt));
        if (cancellingAt > 0) batch.setCancellingAt(Instant.ofEpochSecond(cancellingAt));
        if (cancelledAt > 0) batch.setCancelledAt(Instant.ofEpochSecond(cancelledAt));
        
        JsonNode requestCountsNode = json.get("request_counts");
        if (requestCountsNode != null) {
            RequestCounts counts = new RequestCounts();
            counts.setTotal(JsonUtils.getIntValue(requestCountsNode, "total", 0));
            counts.setCompleted(JsonUtils.getIntValue(requestCountsNode, "completed", 0));
            counts.setFailed(JsonUtils.getIntValue(requestCountsNode, "failed", 0));
            batch.setRequestCounts(counts);
        }
        
        JsonNode metadataNode = json.get("metadata");
        if (metadataNode != null && metadataNode.isObject()) {
            Map<String, String> metadata = new java.util.HashMap<>();
            metadataNode.fields().forEachRemaining(entry -> {
                metadata.put(entry.getKey(), entry.getValue().asText());
            });
            batch.setMetadata(metadata);
        }
    }

    private Request.Builder authorisedRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("User-Agent", "openai-batch-manager/0.1.0");
    }
}