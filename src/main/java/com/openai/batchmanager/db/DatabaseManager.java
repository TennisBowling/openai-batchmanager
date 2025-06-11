package com.openai.batchmanager.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchRequest;
import com.openai.batchmanager.model.BatchStatus;
import com.openai.batchmanager.model.RequestCounts;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private static final String DEFAULT_DB_NAME = "openai_batch_manager.db";

    private final Connection conn;
    private final ObjectMapper mapper;
    private final String dbPath;

    public DatabaseManager() throws SQLException {
        this(DEFAULT_DB_NAME);
    }

    public DatabaseManager(String dbPath) throws SQLException {
        this.dbPath = dbPath;
        String dbUrl = "jdbc:sqlite:" + dbPath;
        this.conn = DriverManager.getConnection(dbUrl);
        this.conn.setAutoCommit(false);
        this.mapper = new ObjectMapper();
        ensureTables();
    }

    private void ensureTables() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS batches (" +
                            "id TEXT PRIMARY KEY," +
                            "openai_batch_id TEXT UNIQUE," +
                            "status TEXT NOT NULL," +
                            "endpoint TEXT," +
                            "input_file_id TEXT," +
                            "output_file_id TEXT," +
                            "error_file_id TEXT," +
                            "completion_window TEXT," +
                            "metadata TEXT," +  // JSON string
                            "request_counts TEXT," +  // JSON string
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "submitted_at TIMESTAMP," +
                            "completed_at TIMESTAMP," +
                            "failed_at TIMESTAMP," +
                            "expired_at TIMESTAMP," +
                            "expires_at TIMESTAMP," +
                            "finalizing_at TIMESTAMP," +
                            "cancelling_at TIMESTAMP," +
                            "cancelled_at TIMESTAMP," +
                            "total_requests INTEGER," +  // legacy field
                            "completed_requests INTEGER" +  // legacy field
                            ")"
            );
            
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS batch_requests (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "batch_id TEXT NOT NULL," +
                            "custom_id TEXT NOT NULL," +
                            "request_data TEXT NOT NULL," +
                            "response_data TEXT," +
                            "status TEXT DEFAULT 'PENDING'," +
                            "FOREIGN KEY (batch_id) REFERENCES batches(id)" +
                            ")"
            );
            
            // Add new columns to existing batches table if they don't exist
            addColumnIfNotExists(stmt, "batches", "endpoint", "TEXT");
            addColumnIfNotExists(stmt, "batches", "input_file_id", "TEXT");
            addColumnIfNotExists(stmt, "batches", "output_file_id", "TEXT");
            addColumnIfNotExists(stmt, "batches", "error_file_id", "TEXT");
            addColumnIfNotExists(stmt, "batches", "completion_window", "TEXT");
            addColumnIfNotExists(stmt, "batches", "metadata", "TEXT");
            addColumnIfNotExists(stmt, "batches", "request_counts", "TEXT");
            addColumnIfNotExists(stmt, "batches", "failed_at", "TIMESTAMP");
            addColumnIfNotExists(stmt, "batches", "expired_at", "TIMESTAMP");
            addColumnIfNotExists(stmt, "batches", "expires_at", "TIMESTAMP");
            addColumnIfNotExists(stmt, "batches", "finalizing_at", "TIMESTAMP");
            addColumnIfNotExists(stmt, "batches", "cancelling_at", "TIMESTAMP");
            addColumnIfNotExists(stmt, "batches", "cancelled_at", "TIMESTAMP");
        }
        conn.commit();
    }
    
    private void addColumnIfNotExists(Statement stmt, String tableName, String columnName, String columnType) {
        try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        } catch (SQLException e) {
            // Check if error is due to column already existing
            if (e.getMessage() != null && e.getMessage().contains("duplicate column name")) {
                // Column already exists, this is expected
                return;
            }
            // Re-throw other SQL exceptions as they indicate real problems
            throw new RuntimeException("Failed to add column " + columnName + " to table " + tableName, e);
        }
    }


    public void insertBatch(Batch batch) throws SQLException {
        final String sql = "INSERT INTO batches(" +
                "id, status, endpoint, completion_window, metadata, request_counts, " +
                "created_at, total_requests, completed_requests) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getId());
            ps.setString(2, batch.getStatus().name());
            ps.setString(3, batch.getEndpoint());
            ps.setString(4, batch.getCompletionWindow());
            ps.setString(5, serializeMetadata(batch.getMetadata()));
            ps.setString(6, serializeRequestCounts(batch.getRequestCounts()));
            ps.setTimestamp(7, Timestamp.from(batch.getCreatedAt()));
            ps.setInt(8, batch.getTotalRequests());
            ps.setInt(9, batch.getCompletedRequests());
            ps.executeUpdate();
        }
        conn.commit();
    }

    public void updateBatch(Batch batch) throws SQLException {
        final String sql = "UPDATE batches SET " +
                "openai_batch_id=?, status=?, endpoint=?, input_file_id=?, output_file_id=?, error_file_id=?, " +
                "completion_window=?, metadata=?, request_counts=?, " +
                "submitted_at=?, completed_at=?, failed_at=?, expired_at=?, expires_at=?, " +
                "finalizing_at=?, cancelling_at=?, cancelled_at=?, " +
                "total_requests=?, completed_requests=? " +
                "WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getOpenaiBatchId());
            ps.setString(2, batch.getStatus().name());
            ps.setString(3, batch.getEndpoint());
            ps.setString(4, batch.getInputFileId());
            ps.setString(5, batch.getOutputFileId());
            ps.setString(6, batch.getErrorFileId());
            ps.setString(7, batch.getCompletionWindow());
            ps.setString(8, serializeMetadata(batch.getMetadata()));
            ps.setString(9, serializeRequestCounts(batch.getRequestCounts()));
            
            // Timestamps
            ps.setTimestamp(10, batch.getSubmittedAt() == null ? null : Timestamp.from(batch.getSubmittedAt()));
            ps.setTimestamp(11, batch.getCompletedAt() == null ? null : Timestamp.from(batch.getCompletedAt()));
            ps.setTimestamp(12, batch.getFailedAt() == null ? null : Timestamp.from(batch.getFailedAt()));
            ps.setTimestamp(13, batch.getExpiredAt() == null ? null : Timestamp.from(batch.getExpiredAt()));
            ps.setTimestamp(14, batch.getExpiresAt() == null ? null : Timestamp.from(batch.getExpiresAt()));
            ps.setTimestamp(15, batch.getFinalizingAt() == null ? null : Timestamp.from(batch.getFinalizingAt()));
            ps.setTimestamp(16, batch.getCancellingAt() == null ? null : Timestamp.from(batch.getCancellingAt()));
            ps.setTimestamp(17, batch.getCancelledAt() == null ? null : Timestamp.from(batch.getCancelledAt()));
            
            // Legacy fields
            ps.setInt(18, batch.getTotalRequests());
            ps.setInt(19, batch.getCompletedRequests());
            
            // WHERE clause
            ps.setString(20, batch.getId());
            
            ps.executeUpdate();
        }
        conn.commit();
    }


    public void insertBatchRequest(String batchId, BatchRequest request) throws SQLException {
        final String sql = "INSERT INTO batch_requests(batch_id, custom_id, request_data, status) " +
                           "VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            ps.setString(2, request.getCustomId());
            ps.setString(3, request.getRequestData());
            ps.setString(4, request.getStatus().name());
            ps.executeUpdate();
        }
        conn.commit();
    }

    public void updateBatchRequest(String batchId, BatchRequest request) throws SQLException {
        final String sql = "UPDATE batch_requests SET response_data=?, status=? " +
                           "WHERE batch_id=? AND custom_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getResponseData());
            ps.setString(2, request.getStatus().name());
            ps.setString(3, batchId);
            ps.setString(4, request.getCustomId());
            ps.executeUpdate();
        }
        conn.commit();
    }


    public List<Batch> getIncompleteBatches() throws SQLException {
        final String sql = "SELECT * FROM batches WHERE status IN ('PENDING','SUBMITTED','VALIDATING','IN_PROGRESS','FINALIZING','CANCELLING')";
        List<Batch> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapBatch(rs));
            }
        }
        return list;
    }

    public List<BatchRequest> getBatchRequests(String batchId) throws SQLException {
        final String sql = "SELECT * FROM batch_requests WHERE batch_id=?";
        List<BatchRequest> requests = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BatchRequest br = new BatchRequest(
                            rs.getString("custom_id"),
                            rs.getString("request_data"));
                    br.setResponseData(rs.getString("response_data"));
                    br.setStatus(BatchRequest.RequestStatus.valueOf(rs.getString("status")));
                    requests.add(br);
                }
            }
        }
        return requests;
    }


    private Batch mapBatch(ResultSet rs) throws SQLException {
        Batch b = new Batch(rs.getString("id"));
        b.setOpenaiBatchId(rs.getString("openai_batch_id"));
        b.setStatus(BatchStatus.valueOf(rs.getString("status")));
        b.setEndpoint(rs.getString("endpoint"));
        b.setInputFileId(rs.getString("input_file_id"));
        b.setOutputFileId(rs.getString("output_file_id"));
        b.setErrorFileId(rs.getString("error_file_id"));
        b.setCompletionWindow(rs.getString("completion_window"));

        // Deserialize metadata and request counts
        b.setMetadata(deserializeMetadata(rs.getString("metadata")));
        b.setRequestCounts(deserializeRequestCounts(rs.getString("request_counts")));

        // Map timestamps
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) b.setSubmittedAt(submitted.toInstant());

        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) b.setCompletedAt(completed.toInstant());

        Timestamp failed = rs.getTimestamp("failed_at");
        if (failed != null) b.setFailedAt(failed.toInstant());

        Timestamp expired = rs.getTimestamp("expired_at");
        if (expired != null) b.setExpiredAt(expired.toInstant());

        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) b.setExpiresAt(expires.toInstant());

        Timestamp finalizing = rs.getTimestamp("finalizing_at");
        if (finalizing != null) b.setFinalizingAt(finalizing.toInstant());

        Timestamp cancelling = rs.getTimestamp("cancelling_at");
        if (cancelling != null) b.setCancellingAt(cancelling.toInstant());

        Timestamp cancelled = rs.getTimestamp("cancelled_at");
        if (cancelled != null) b.setCancelledAt(cancelled.toInstant());

        // Legacy fields
        b.setTotalRequests(rs.getInt("total_requests"));
        b.setCompletedRequests(rs.getInt("completed_requests"));
        
        return b;
    }


    private String serializeMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    private Map<String, String> deserializeMetadata(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize metadata", e);
        }
    }

    private String serializeRequestCounts(RequestCounts counts) {
        if (counts == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(counts);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request counts", e);
        }
    }

    private RequestCounts deserializeRequestCounts(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new RequestCounts();
        }
        try {
            return mapper.readValue(json, RequestCounts.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize request counts", e);
        }
    }


    public void clearDatabase() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Drop tables in correct order (child tables first due to foreign keys)
            stmt.executeUpdate("DROP TABLE IF EXISTS batch_requests");
            stmt.executeUpdate("DROP TABLE IF EXISTS batches");
        }
        conn.commit();
        
        ensureTables();
    }

    public String getDatabasePath() {
        return dbPath;
    }
}