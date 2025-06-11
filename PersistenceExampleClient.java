import com.openai.batchmanager.manager.BatchManager;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchStatus;
import com.openai.batchmanager.util.JsonUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PersistenceExampleClient {
    
    private static final String API_KEY = "your-openai-api-key-here";
    
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--resume")) {
            resumeExample();
        } else {
            submitExample();
        }
    }
    
    private static void submitExample() {
        
        try (BatchManager manager = new BatchManager(API_KEY)) {
            
            // Prepare two requests for the batch
            Map<String, String> requests = new HashMap<>();
            
            requests.put("physics-question", """
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [
                        {"role": "user", "content": "Explain the concept of quantum entanglement in simple terms."}
                    ]
                }
                """);
            
            requests.put("math-question", """
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [
                        {"role": "user", "content": "What is the Fibonacci sequence and how is it calculated?"}
                    ]
                }
                """);
            
            // Add metadata to track this example
            Map<String, String> metadata = new HashMap<>();
            metadata.put("example_type", "persistence_demo");
            metadata.put("client", "PersistenceExampleClient");
            metadata.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            
            // Submit the batch but don't wait for completion
            CompletableFuture<Map<String, String>> future = manager.submitAsync(requests, metadata);
            
            // Give it a moment to submit and start processing
            Thread.sleep(3000);
            
            // Check what batches are currently in progress
            List<Batch> incompleteBatches = manager.getIncompleteBatches();
            
            
            // Exit without waiting for completion to simulate interruption
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit example batch", e);
        }
    }
    
    private static void resumeExample() {
        
        try (BatchManager manager = new BatchManager(API_KEY)) {
            
            // Check for incomplete batches in the database
            List<Batch> incompleteBatches = manager.getIncompleteBatches();
            
            if (incompleteBatches.isEmpty()) {
                System.out.println("No incomplete batches found in database.");
                System.out.println("Run without arguments first to submit a batch:");
                System.out.println("java -cp .:target/classes:$(cat classpath.txt) PersistenceExampleClient");
                return;
            }
            
            
            for (Batch batch : incompleteBatches) {
                
                if (batch.getStatus().isTerminal()) {
                    System.out.println("Batch is already in terminal state: " + batch.getStatus());
                    continue;
                }
                
                // Resume monitoring this batch
                
                CompletableFuture<Map<String, String>> future = manager.resumeBatchAsync(batch);
                
                
                try {
                    Map<String, String> results = future.get();
                    results.forEach((requestId, answer) -> {
                        System.out.println(requestId + ": " + answer);
                    });
                    
                } catch (Exception e) {
                    System.err.println("Failed to complete batch " + batch.getOpenaiBatchId() + ": " + e.getMessage());
                }
            }
            
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to resume example", e);
        }
    }
}