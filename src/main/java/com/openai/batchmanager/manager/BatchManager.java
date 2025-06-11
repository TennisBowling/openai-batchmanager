package com.openai.batchmanager.manager;

import com.openai.batchmanager.client.OpenAIClient;
import com.openai.batchmanager.db.DatabaseManager;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchRequest;
import com.openai.batchmanager.service.BatchService;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchManager implements AutoCloseable {

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(20);

    private final BatchService service;
    private final ExecutorService executor;
    private final Duration pollInterval;

    public BatchManager(String openAiApiKey) throws SQLException {
        this(openAiApiKey, new DatabaseManager(), Executors.newCachedThreadPool(), DEFAULT_POLL_INTERVAL);
    }

    public BatchManager(String openAiApiKey, DatabaseManager databaseManager) throws SQLException {
        this(openAiApiKey, databaseManager, Executors.newCachedThreadPool(), DEFAULT_POLL_INTERVAL);
    }

    public BatchManager(String openAiApiKey,
                        DatabaseManager databaseManager,
                        ExecutorService executor,
                        Duration pollInterval) throws SQLException {

        this.executor     = executor;
        this.pollInterval = pollInterval;

        OpenAIClient client = new OpenAIClient(openAiApiKey);
        this.service = new BatchService(databaseManager, client);
    }

    public CompletableFuture<Map<String, String>> submitAsync(Map<String, String> customIdToRequestJson) {
        return submitAsync(customIdToRequestJson, null);
    }

    public CompletableFuture<Map<String, String>> submitAsync(Map<String, String> customIdToRequestJson, Map<String, String> metadata) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                Batch batch = new Batch(UUID.randomUUID().toString());

                List<BatchRequest> reqs = customIdToRequestJson.entrySet()
                        .stream()
                        .map(e -> new BatchRequest(e.getKey(), e.getValue()))
                        .toList();

                if (metadata != null && !metadata.isEmpty()) {
                    service.submitBatchWithMetadata(batch, reqs, metadata);
                } else {
                    service.submitBatch(batch, reqs);
                }

                // Poll until complete
                while (true) {
                    if (service.pollBatchStatus(batch)) {
                        Map<String, String> results = service.fetchResults(batch, reqs);
                        future.complete(results);
                        break;
                    }
                    Thread.sleep(pollInterval.toMillis());
                }
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    public CompletableFuture<Void> cancelBatchAsync(String batchId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                List<Batch> incompleteBatches = service.getIncompleteBatches();
                Batch batchToCancel = incompleteBatches.stream()
                        .filter(b -> batchId.equals(b.getOpenaiBatchId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

                service.cancelBatch(batchToCancel);
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    public CompletableFuture<String> listBatchesAsync(Integer limit, String after) {
        CompletableFuture<String> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                String result = service.listBatches(limit, after);
                future.complete(result);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    public List<Batch> getIncompleteBatches() throws SQLException {
        return service.getIncompleteBatches();
    }

    public CompletableFuture<Map<String, String>> resumeBatchAsync(Batch batch) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

        executor.submit(() -> {
            try {
                List<BatchRequest> requests = service.getBatchRequests(batch.getId());

                // Poll until complete
                while (true) {
                    if (service.pollBatchStatus(batch)) {
                        Map<String, String> results = service.fetchResults(batch, requests);
                        future.complete(results);
                        break;
                    }
                    Thread.sleep(pollInterval.toMillis());
                }
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        return future;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}