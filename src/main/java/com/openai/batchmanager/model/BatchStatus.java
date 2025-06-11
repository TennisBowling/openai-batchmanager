package com.openai.batchmanager.model;

public enum BatchStatus {
    PENDING,
    
    VALIDATING,
    FAILED,
    IN_PROGRESS,
    FINALIZING,
    COMPLETED,
    EXPIRED,
    CANCELLING,
    CANCELLED,
    SUBMITTED;
    
    public static BatchStatus fromOpenAIStatus(String apiStatus) {
        return switch (apiStatus.toLowerCase()) {
            case "validating" -> VALIDATING;
            case "failed" -> FAILED;
            case "in_progress" -> IN_PROGRESS;
            case "finalizing" -> FINALIZING;
            case "completed" -> COMPLETED;
            case "expired" -> EXPIRED;
            case "cancelling" -> CANCELLING;
            case "cancelled" -> CANCELLED;
            default -> throw new IllegalArgumentException("Unknown OpenAI batch status: " + apiStatus);
        };
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED || this == CANCELLED;
    }
}