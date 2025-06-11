package com.openai.batchmanager;

import com.openai.batchmanager.db.DatabaseManager;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchRequest;
import com.openai.batchmanager.model.BatchStatus;
import com.openai.batchmanager.model.RequestCounts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @TempDir
    Path tempDir;

    private DatabaseManager dbManager;
    private String testDbPath;

    @BeforeEach
    void setUp() throws SQLException {
        testDbPath = tempDir.resolve("test_database.db").toString();
        dbManager = new DatabaseManager(testDbPath);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dbManager != null) {
            dbManager.clearDatabase();
        }
    }

    @Test
    void testBatchCRUD() throws SQLException {
        // Create a test batch with unique ID
        String batchId = "test-batch-" + System.currentTimeMillis();
        Batch batch = new Batch(batchId);
        batch.setTotalRequests(5);
        batch.setCompletedRequests(0);

        dbManager.insertBatch(batch);

        // Update batch
        batch.setOpenaiBatchId("openai-" + System.currentTimeMillis());
        batch.setStatus(BatchStatus.SUBMITTED);
        dbManager.updateBatch(batch);

        // Verify by querying incomplete batches
        List<Batch> incompleteBatches = dbManager.getIncompleteBatches();
        assertTrue(incompleteBatches.stream().anyMatch(b -> b.getId().equals(batchId)));
    }

    @Test
    void testBatchRequestCRUD() throws SQLException {
        // Create a test batch first with unique ID
        String batchId = "test-batch-req-" + System.currentTimeMillis();
        Batch batch = new Batch(batchId);
        dbManager.insertBatch(batch);

        // Create and insert a batch request
        BatchRequest request = new BatchRequest("custom-001", "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"Hello\"}]}");
        dbManager.insertBatchRequest(batch.getId(), request);

        // Update the request with a response
        request.setResponseData("{\"choices\": [{\"message\": {\"content\": \"Hello there!\"}}]}");
        request.setStatus(BatchRequest.RequestStatus.COMPLETED);
        dbManager.updateBatchRequest(batch.getId(), request);

        // Retrieve and verify
        List<BatchRequest> requests = dbManager.getBatchRequests(batch.getId());
        assertEquals(1, requests.size());
        assertEquals("custom-001", requests.get(0).getCustomId());
        assertEquals(BatchRequest.RequestStatus.COMPLETED, requests.get(0).getStatus());
        assertNotNull(requests.get(0).getResponseData());
    }

    @Test
    void testCustomDatabasePath() throws SQLException {
        // Test that the database manager uses the correct path
        assertEquals(testDbPath, dbManager.getDatabasePath());
        
        // Verify the database file exists
        File dbFile = new File(testDbPath);
        assertTrue(dbFile.exists(), "Database file should exist after initialization");
    }

    @Test
    void testClearDatabase() throws SQLException {
        // Insert some test data
        String batchId = "test-batch-" + System.currentTimeMillis();
        Batch batch = new Batch(batchId);
        batch.setTotalRequests(5);
        batch.setCompletedRequests(0);
        dbManager.insertBatch(batch);

        // Insert a batch request
        BatchRequest request = new BatchRequest("custom-001", "{\"test\": \"data\"}");
        dbManager.insertBatchRequest(batchId, request);

        // Verify data exists
        List<Batch> batchesBefore = dbManager.getIncompleteBatches();
        List<BatchRequest> requestsBefore = dbManager.getBatchRequests(batchId);
        assertFalse(batchesBefore.isEmpty(), "Should have batches before clearing");
        assertFalse(requestsBefore.isEmpty(), "Should have requests before clearing");

        // Clear the database
        dbManager.clearDatabase();

        // Verify data is gone
        List<Batch> batchesAfter = dbManager.getIncompleteBatches();
        List<BatchRequest> requestsAfter = dbManager.getBatchRequests(batchId);
        assertTrue(batchesAfter.isEmpty(), "Should have no batches after clearing");
        assertTrue(requestsAfter.isEmpty(), "Should have no requests after clearing");
    }

    @Test
    void testBatchWithAllFields() throws SQLException {
        String batchId = "comprehensive-batch-" + System.currentTimeMillis();
        Batch batch = new Batch(batchId);
        
        // Set all possible fields
        batch.setOpenaiBatchId("openai-batch-123");
        batch.setStatus(BatchStatus.IN_PROGRESS);
        batch.setEndpoint("/v1/chat/completions");
        batch.setInputFileId("input-file-123");
        batch.setOutputFileId("output-file-123");
        batch.setErrorFileId("error-file-123");
        batch.setCompletionWindow("24h");
        batch.setTotalRequests(100);
        batch.setCompletedRequests(50);
        
        // Set metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("project", "test-project");
        metadata.put("version", "1.0");
        batch.setMetadata(metadata);
        
        // Set request counts
        RequestCounts counts = new RequestCounts();
        counts.setTotal(100);
        counts.setCompleted(50);
        counts.setFailed(5);
        batch.setRequestCounts(counts);
        
        // Set timestamps
        Instant now = Instant.now();
        batch.setSubmittedAt(now);
        batch.setCompletedAt(now.plusSeconds(3600));
        
        dbManager.insertBatch(batch);
        
        // Update batch with more fields
        batch.setFailedAt(now.plusSeconds(1800));
        batch.setExpiresAt(now.plusSeconds(86400));
        dbManager.updateBatch(batch);
        
        // Retrieve and verify
        List<Batch> batches = dbManager.getIncompleteBatches();
        Batch retrieved = batches.stream()
            .filter(b -> b.getId().equals(batchId))
            .findFirst()
            .orElse(null);
        
        assertNotNull(retrieved, "Batch should be found");
        assertEquals("openai-batch-123", retrieved.getOpenaiBatchId());
        assertEquals(BatchStatus.IN_PROGRESS, retrieved.getStatus());
        assertEquals("/v1/chat/completions", retrieved.getEndpoint());
        assertEquals("input-file-123", retrieved.getInputFileId());
        assertEquals("output-file-123", retrieved.getOutputFileId());
        assertEquals("error-file-123", retrieved.getErrorFileId());
        assertEquals("24h", retrieved.getCompletionWindow());
        assertEquals(100, retrieved.getTotalRequests());
        assertEquals(50, retrieved.getCompletedRequests());
        
        // Verify metadata
        assertNotNull(retrieved.getMetadata());
        assertEquals("test-project", retrieved.getMetadata().get("project"));
        assertEquals("1.0", retrieved.getMetadata().get("version"));
        
        // Verify request counts
        assertNotNull(retrieved.getRequestCounts());
        assertEquals(100, retrieved.getRequestCounts().getTotal());
        assertEquals(50, retrieved.getRequestCounts().getCompleted());
        assertEquals(5, retrieved.getRequestCounts().getFailed());
        
        // Verify timestamps
        assertNotNull(retrieved.getSubmittedAt());
        assertNotNull(retrieved.getCompletedAt());
        assertNotNull(retrieved.getFailedAt());
        assertNotNull(retrieved.getExpiresAt());
    }

    @Test
    void testBatchRequestStatuses() throws SQLException {
        String batchId = "status-test-batch-" + System.currentTimeMillis();
        Batch batch = new Batch(batchId);
        dbManager.insertBatch(batch);

        // Test all request statuses
        BatchRequest.RequestStatus[] statuses = BatchRequest.RequestStatus.values();
        
        for (int i = 0; i < statuses.length; i++) {
            BatchRequest request = new BatchRequest("custom-" + i, "{\"test\": " + i + "}");
            request.setStatus(statuses[i]);
            dbManager.insertBatchRequest(batchId, request);
        }

        // Retrieve and verify all requests
        List<BatchRequest> requests = dbManager.getBatchRequests(batchId);
        assertEquals(statuses.length, requests.size());
        
        for (int i = 0; i < statuses.length; i++) {
            final int index = i;
            BatchRequest request = requests.stream()
                .filter(r -> r.getCustomId().equals("custom-" + index))
                .findFirst()
                .orElse(null);
            assertNotNull(request);
            assertEquals(statuses[i], request.getStatus());
        }
    }

    @Test
    void testMultipleDatabaseInstances() throws SQLException {
        // Create another database manager with different path
        String secondDbPath = tempDir.resolve("second_database.db").toString();
        DatabaseManager secondDbManager = new DatabaseManager(secondDbPath);
        
        try {
            // Insert data in first database
            String batchId1 = "batch-db1-" + System.currentTimeMillis();
            Batch batch1 = new Batch(batchId1);
            dbManager.insertBatch(batch1);
            
            // Insert data in second database
            String batchId2 = "batch-db2-" + System.currentTimeMillis();
            Batch batch2 = new Batch(batchId2);
            secondDbManager.insertBatch(batch2);
            
            // Verify data isolation
            List<Batch> batches1 = dbManager.getIncompleteBatches();
            List<Batch> batches2 = secondDbManager.getIncompleteBatches();
            
            assertEquals(1, batches1.size());
            assertEquals(1, batches2.size());
            assertEquals(batchId1, batches1.get(0).getId());
            assertEquals(batchId2, batches2.get(0).getId());
            
            // Verify different paths
            assertNotEquals(dbManager.getDatabasePath(), secondDbManager.getDatabasePath());
            
        } finally {
            secondDbManager.clearDatabase();
        }
    }

    @Test
    void testEmptyResultSets() throws SQLException {
        // Test queries on empty database
        List<Batch> batches = dbManager.getIncompleteBatches();
        assertTrue(batches.isEmpty());
        
        List<BatchRequest> requests = dbManager.getBatchRequests("non-existent-batch");
        assertTrue(requests.isEmpty());
    }

    @Test
    void testBatchStatusFiltering() throws SQLException {
        // Create batches with different statuses
        BatchStatus[] testStatuses = {
            BatchStatus.PENDING,
            BatchStatus.SUBMITTED,
            BatchStatus.IN_PROGRESS,
            BatchStatus.COMPLETED,
            BatchStatus.FAILED,
            BatchStatus.CANCELLED
        };
        
        for (int i = 0; i < testStatuses.length; i++) {
            Batch batch = new Batch("batch-" + testStatuses[i] + "-" + System.currentTimeMillis());
            batch.setStatus(testStatuses[i]);
            dbManager.insertBatch(batch);
        }
        
        // getIncompleteBatches should only return non-terminal statuses
        List<Batch> incompleteBatches = dbManager.getIncompleteBatches();
        
        // Count expected incomplete statuses (PENDING, SUBMITTED, IN_PROGRESS, etc.)
        // Based on the SQL query: 'PENDING','SUBMITTED','VALIDATING','IN_PROGRESS','FINALIZING','CANCELLING'
        long expectedIncomplete = incompleteBatches.size();
        assertTrue(expectedIncomplete > 0, "Should have some incomplete batches");
        
        // Verify no completed, failed, or cancelled batches are returned
        for (Batch batch : incompleteBatches) {
            assertNotEquals(BatchStatus.COMPLETED, batch.getStatus());
            assertNotEquals(BatchStatus.FAILED, batch.getStatus());
            assertNotEquals(BatchStatus.CANCELLED, batch.getStatus());
        }
    }

}