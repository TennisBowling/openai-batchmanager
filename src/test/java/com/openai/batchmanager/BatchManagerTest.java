package com.openai.batchmanager;

import com.openai.batchmanager.manager.BatchManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class BatchManagerTest {

    private BatchManager batchManager;

    @BeforeEach
    void setUp() throws SQLException {
        batchManager = new BatchManager("your-openai-api-key-here");
    }

    @AfterEach
    void tearDown() {
        if (batchManager != null) {
            batchManager.close();
        }
    }

    @Test
    void testSubmitAsync() throws Exception {
        Map<String, String> requests = new HashMap<>();
        requests.put("req-001", "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"Hello\"}]}");
        requests.put("req-002", "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"How are you?\"}]}");

        CompletableFuture<Map<String, String>> future = batchManager.submitAsync(requests);

        assertNotNull(future, "Future should not be null");
        assertFalse(future.isDone(), "Future should not be completed immediately as batch processing takes time");
        assertFalse(future.isCancelled(), "Future should not be cancelled");
        
        System.out.println("Batch submitted successfully. Processing will continue asynchronously.");
    }

    @Test
    void testBatchManagerCloseable() throws SQLException {
        BatchManager manager = new BatchManager("test-key");
        assertDoesNotThrow(() -> manager.close());
    }

    @Test
    void testGetIncompleteBatches() throws SQLException {
        assertDoesNotThrow(() -> {
            var incompleteBatches = batchManager.getIncompleteBatches();
            assertNotNull(incompleteBatches);
        });
    }

    @Test
    void testSubmitAsyncWithMetadata() {
        Map<String, String> requests = new HashMap<>();
        requests.put("test-req", "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"Test\"}]}");
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("test_run", "unit_test");
        
        assertDoesNotThrow(() -> {
            CompletableFuture<Map<String, String>> future = batchManager.submitAsync(requests, metadata);
            assertNotNull(future);
            
        });
    }
}