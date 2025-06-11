package com.openai.batchmanager.model;

public class RequestCounts {
    private int total;
    private int completed;
    private int failed;

    public RequestCounts() {
    }

    public RequestCounts(int total, int completed, int failed) {
        this.total = total;
        this.completed = completed;
        this.failed = failed;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    @Override
    public String toString() {
        return "RequestCounts{" +
                "total=" + total +
                ", completed=" + completed +
                ", failed=" + failed +
                '}';
    }
}