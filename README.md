# OpenAI Batch Manager
Java library for OpenAI's Batch API - Lets you use the 50% cost reduction simply.

## What it is

This library simplifies OpenAI batch operations by handling file uploads, status monitoring, result retrieval automatically, and request to response ID matching. It provides persistent state management and async processing for large-scale AI requests.

## How it simplifies batch processing

- Automatic JSONL file creation and management
- Built-in polling and status tracking
- SQLite persistence for crash recovery
- Simple async API with CompletableFuture
- Matches request IDs to response IDs

## Add as dependency in Maven
Technically this library is not published on Maven Central but that is the next step to make this easier:  

```xml
<dependency>
    <groupId>com.openai.batchmanager</groupId>
    <artifactId>openai-batch-manager</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Example usage

```java
import com.openai.batchmanager.manager.BatchManager;
import java.util.Map;
import java.util.HashMap;

public class Example {
    public static void main(String[] args) throws Exception {
        try (BatchManager manager = new BatchManager("your-openai-api-key")) {
            Map<String, String> requests = new HashMap<>();
            requests.put("req1", """
                {
                    "model": "gpt-4.1",
                    "messages": [{"role": "user", "content": "Who are you?"}]
                }
                """);
            
            Map<String, String> results = manager.submitAsync(requests).get();
            results.forEach((id, response) -> System.out.println(id + ": " + response));
        }
    }
}
```

## Key methods

- `submitAsync(requests)` - Submit batch and return CompletableFuture
- `submitAsync(requests, metadata)` - Submit with custom metadata
- `cancelBatchAsync(batchId)` - Cancel running batch
- `getIncompleteBatches()` - Resume interrupted batches
- `listBatchesAsync(limit, after)` - List all batches

## Requirements

- Java 17+
- OpenAI API key