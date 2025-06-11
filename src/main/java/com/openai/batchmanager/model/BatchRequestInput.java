package com.openai.batchmanager.model;

public class BatchRequestInput {
    private String custom_id;
    private String method;
    private String url;
    private Object body;

    public BatchRequestInput() {
    }

    public BatchRequestInput(String customId, String method, String url, Object body) {
        if (customId == null || customId.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom ID cannot be null or empty");
        }
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Method cannot be null or empty");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        
        this.custom_id = customId.trim();
        this.method = method.trim().toUpperCase();
        this.url = url.trim();
        this.body = body;
    }

    public static BatchRequestInput forChatCompletion(String customId, Object requestBody) {
        return new BatchRequestInput(customId, "POST", "/v1/chat/completions", requestBody);
    }

    public static BatchRequestInput forEmbeddings(String customId, Object requestBody) {
        return new BatchRequestInput(customId, "POST", "/v1/embeddings", requestBody);
    }

    public static BatchRequestInput forCompletions(String customId, Object requestBody) {
        return new BatchRequestInput(customId, "POST", "/v1/completions", requestBody);
    }

    public String getCustom_id() {
        return custom_id;
    }

    public void setCustom_id(String custom_id) {
        if (custom_id == null || custom_id.trim().isEmpty()) {
            throw new IllegalArgumentException("Custom ID cannot be null or empty");
        }
        this.custom_id = custom_id.trim();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("Method cannot be null or empty");
        }
        this.method = method.trim().toUpperCase();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        this.url = url.trim();
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        if (body == null) {
            throw new IllegalArgumentException("Body cannot be null");
        }
        this.body = body;
    }

    @Override
    public String toString() {
        return "BatchRequestInput{" +
                "custom_id='" + custom_id + '\'' +
                ", method='" + method + '\'' +
                ", url='" + url + '\'' +
                ", body=" + body +
                '}';
    }
}