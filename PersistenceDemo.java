import com.openai.batchmanager.manager.BatchManager;
import com.openai.batchmanager.model.Batch;
import com.openai.batchmanager.model.BatchStatus;
import com.openai.batchmanager.util.JsonUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PersistenceDemo {
    
    private static final String API_KEY = "your-openai-api-key-here";
    
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--resume")) {
            resumeDemo();
        } else {
            submitDemo();
        }
    }
    
    private static void submitDemo() {
        try (BatchManager manager = new BatchManager(API_KEY)) {
            
            // Create two substantial requests that should take some time to process
            Map<String, String> requests = new HashMap<>();
            
            requests.put("physics-essay", """
                {
                    "model": "o4-mini",
                    "messages": [
                        {"role": "user", "content": "Write a detailed 500-word essay explaining quantum entanglement, its implications for physics, and potential applications in quantum computing and communication. Include examples and explain why Einstein called it 'spooky action at a distance'."}
                    ],
                    "max_tokens": 800
                }
                """);
            
            requests.put("math-tutorial", """
                {
                    "model": "o4-mini", 
                    "messages": [
                        {"role": "user", "content": "Create a comprehensive tutorial explaining the Fibonacci sequence. Include: 1) Mathematical definition, 2) First 20 numbers in the sequence, 3) The golden ratio connection, 4) Real-world examples where Fibonacci appears in nature, 5) Programming implementation in Python, and 6) Historical background about Leonardo Fibonacci."}
                    ],
                    "max_tokens": 1000
                }
                """);
            
            // Add metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("demo_type", "persistence_test");
            metadata.put("client", "PersistenceDemo");
            metadata.put("submission_time", String.valueOf(System.currentTimeMillis()));
            
            
            // Submit asynchronously but don't wait
            CompletableFuture<Map<String, String>> future = manager.submitAsync(requests, metadata);
            
            // Give it time to submit to OpenAI
            Thread.sleep(5000);
            
            // Check database state
            List<Batch> batches = manager.getIncompleteBatches();
            
            
            // Exit without waiting - simulating crash/restart
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit demo batch", e);
        }
    }
    
    private static void resumeDemo() {
        try (BatchManager manager = new BatchManager(API_KEY)) {
            
            List<Batch> batches = manager.getIncompleteBatches();
            
            if (batches.isEmpty()) {
                System.out.println("No incomplete batches found in database.");
                System.out.println("Run without arguments first to submit a batch.");
                return;
            }
            
            System.out.println("Found " + batches.size() + " batch(es) to resume:");
            
            for (Batch batch : batches) {
                
                if (batch.getOpenaiBatchId() == null || batch.getStatus().isTerminal()) {
                    continue;
                }
                
                try {
                    CompletableFuture<Map<String, String>> future = manager.resumeBatchAsync(batch);
                    
                    // Wait with timeout
                    Map<String, String> results = future.get(2, TimeUnit.MINUTES);
                    results.forEach((requestId, answer) -> {
                        System.out.println(requestId + ": " + answer);
                    });
                    
                } catch (Exception e) {
                    System.err.println("Failed to resume batch " + batch.getOpenaiBatchId() + ": " + e.getMessage());
                }
            }
            
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to resume demo", e);
        }
    }
}