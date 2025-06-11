import com.openai.batchmanager.manager.BatchManager;
import com.openai.batchmanager.util.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ExampleClient {
    public static void main(String[] args) throws Exception {
        try (BatchManager batchManager = new BatchManager("your-openai-api-key-here")) {
            
            Map<String, String> requests = new HashMap<>();
            String requestJson = "{\n" +
                "  \"model\": \"gpt-4.1-nano\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": \"Hello, how are you today?\"}\n" +
                "  ]\n" +
                "}";
            
            requests.put("hello-request", requestJson);
            
            CompletableFuture<Map<String, String>> future = batchManager.submitAsync(requests);
            Map<String, String> results = future.get();

            results.forEach((customId, responseJson) -> {
                System.out.println("Request ID: " + customId);
                System.out.println("Response: " + responseJson);
                System.out.println();
            });
            
            Map<String, JsonUtils.RequestResponsePair> answers = JsonUtils.extractAnswers(requests, results);
            answers.forEach((customId, pair) -> {
                System.out.println("Question: " + pair.getRequest());
                System.out.println("Answer: " + pair.getResponse());
                System.out.println();
            });
        }
    }
}