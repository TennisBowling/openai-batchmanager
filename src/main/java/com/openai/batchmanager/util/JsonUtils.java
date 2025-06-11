package com.openai.batchmanager.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.batchmanager.model.BatchRequestInput;
import com.openai.batchmanager.model.BatchRequestOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {
    
    private static final ObjectMapper mapper = createSecureObjectMapper();
    
    private static ObjectMapper createSecureObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
                .build();
    }
    
    public static String createBatchInputJsonl(List<String> requestJsonList, String endpoint) throws JsonProcessingException {
        if (requestJsonList == null || requestJsonList.isEmpty()) {
            throw new IllegalArgumentException("Request list cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        StringBuilder jsonl = new StringBuilder();
        
        for (int i = 0; i < requestJsonList.size(); i++) {
            String requestJson = requestJsonList.get(i);
            if (requestJson == null || requestJson.trim().isEmpty()) {
                throw new IllegalArgumentException("Request JSON at index " + i + " cannot be null or empty");
            }
            
            String customId = "request-" + (i + 1);
            
            // Parse the request JSON to get the body
            JsonNode requestBody = mapper.readTree(requestJson);
            
            // Create the batch request input
            BatchRequestInput input = new BatchRequestInput(customId, "POST", endpoint, requestBody);
            
            // Convert to JSON and append to JSONL
            jsonl.append(mapper.writeValueAsString(input));
            jsonl.append("\n");
        }
        
        // Remove the last newline for proper JSONL format
        if (jsonl.length() > 0 && jsonl.charAt(jsonl.length() - 1) == '\n') {
            jsonl.setLength(jsonl.length() - 1);
        }
        
        return jsonl.toString();
    }
    
    public static String createBatchInputJsonlWithCustomIds(java.util.Map<String, String> customIdToRequestJson, String endpoint) throws JsonProcessingException {
        if (customIdToRequestJson == null || customIdToRequestJson.isEmpty()) {
            throw new IllegalArgumentException("CustomId to request JSON map cannot be null or empty");
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        StringBuilder jsonl = new StringBuilder();
        boolean first = true;
        
        for (java.util.Map.Entry<String, String> entry : customIdToRequestJson.entrySet()) {
            String customId = entry.getKey();
            String requestJson = entry.getValue();
            
            if (customId == null || customId.trim().isEmpty()) {
                throw new IllegalArgumentException("Custom ID cannot be null or empty");
            }
            if (requestJson == null || requestJson.trim().isEmpty()) {
                throw new IllegalArgumentException("Request JSON for custom ID '" + customId + "' cannot be null or empty");
            }
            
            if (!first) {
                jsonl.append("\n");
            }
            first = false;
            
            // Parse the request JSON to get the body
            JsonNode requestBody = mapper.readTree(requestJson);
            
            // Create the batch request input
            BatchRequestInput input = new BatchRequestInput(customId, "POST", endpoint, requestBody);
            
            // Convert to JSON and append to JSONL
            jsonl.append(mapper.writeValueAsString(input));
        }
        
        return jsonl.toString();
    }
    
    public static List<BatchRequestOutput> parseBatchOutputJsonl(String jsonlContent) throws IOException {
        List<BatchRequestOutput> outputs = new ArrayList<>();
        
        if (jsonlContent == null || jsonlContent.trim().isEmpty()) {
            return outputs;
        }
        
        String[] lines = jsonlContent.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            
            BatchRequestOutput output = mapper.readValue(line, BatchRequestOutput.class);
            outputs.add(output);
        }
        
        return outputs;
    }
    
    public static java.util.Map<String, String> outputsToResponseMap(List<BatchRequestOutput> outputs) throws JsonProcessingException {
        java.util.Map<String, String> responseMap = new java.util.HashMap<>();
        
        for (BatchRequestOutput output : outputs) {
            if (output.isSuccess() && output.getResponse() != null) {
                String responseJson = mapper.writeValueAsString(output.getResponse().getBody());
                responseMap.put(output.getCustom_id(), responseJson);
            }
        }
        
        return responseMap;
    }
    
    public static java.util.Map<String, String> extractAnswers(java.util.Map<String, String> results) {
        java.util.Map<String, String> answers = new java.util.HashMap<>();
        
        results.forEach((customId, responseJson) -> {
            try {
                JsonNode response = mapper.readTree(responseJson);
                String content = response.get("choices").get(0).get("message").get("content").asText();
                answers.put(customId, content);
            } catch (Exception e) {
                answers.put(customId, "Error parsing response: " + e.getMessage());
            }
        });
        
        return answers;
    }
    
    public static class RequestResponsePair {
        private final String request;
        private final String response;
        
        public RequestResponsePair(String request, String response) {
            this.request = request;
            this.response = response;
        }
        
        public String getRequest() {
            return request;
        }
        
        public String getResponse() {
            return response;
        }
        
        @Override
        public String toString() {
            return "RequestResponsePair{" +
                    "request='" + request + '\'' +
                    ", response='" + response + '\'' +
                    '}';
        }
    }
    
    public static java.util.Map<String, RequestResponsePair> extractAnswers(
            java.util.Map<String, String> requestData,
            java.util.Map<String, String> responseResults) {
        java.util.Map<String, RequestResponsePair> pairs = new java.util.HashMap<>();
        
        responseResults.forEach((customId, responseJson) -> {
            try {
                // Extract response content
                JsonNode response = mapper.readTree(responseJson);
                String responseContent = response.get("choices").get(0).get("message").get("content").asText();
                
                // Extract request content from original request data
                String requestContent = null;
                String originalRequestJson = requestData.get(customId);
                if (originalRequestJson != null) {
                    JsonNode request = mapper.readTree(originalRequestJson);
                    // For chat completions, extract the user message content
                    JsonNode messages = request.get("messages");
                    if (messages != null && messages.isArray() && messages.size() > 0) {
                        // Find the last user message (typically the main request)
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            JsonNode message = messages.get(i);
                            if ("user".equals(message.get("role").asText())) {
                                requestContent = message.get("content").asText();
                                break;
                            }
                        }
                    }
                }
                
                if (requestContent == null) {
                    requestContent = "Unable to extract request content";
                }
                
                pairs.put(customId, new RequestResponsePair(requestContent, responseContent));
            } catch (Exception e) {
                String errorMsg = "Error parsing: " + e.getMessage();
                pairs.put(customId, new RequestResponsePair(errorMsg, errorMsg));
            }
        });
        
        return pairs;
    }
    
    public static ObjectNode parseBatchResponse(String jsonResponse) throws JsonProcessingException {
        return (ObjectNode) mapper.readTree(jsonResponse);
    }
    
    public static String getStringValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asText() : null;
    }
    
    public static int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asInt() : defaultValue;
    }
    
    public static long getLongValue(JsonNode node, String fieldName, long defaultValue) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() ? field.asLong() : defaultValue;
    }
    
    public static String toPrettyString(Object obj) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }
    
    public static ObjectMapper getMapper() {
        return mapper;
    }
}