/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package main.java.com.mindemia.codeinsert.completion;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.java.com.mindemia.codeinsert.AICompletionClient;
import main.java.com.mindemia.codeinsert.AICompletionOptionsPanel;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.openide.util.NbPreferences;

/**
 *
 * @author rickard
 */
public class AiFimClient extends AICompletionClient{
    
    private static final String API_URL = NbPreferences.forModule(AICompletionOptionsPanel.class).get("host_fim", "http://127.0.0.1:8080/v1/completions");
    private static final String API_KEY = NbPreferences.forModule(AICompletionOptionsPanel.class).get("api_key", "");
    private static final String MODEL = NbPreferences.forModule(AICompletionOptionsPanel.class).get("model_fim", "gpt-4");
    private static final int MAX_TOKENS = NbPreferences.forModule(AICompletionOptionsPanel.class).getInt("max_tokens", 300);
    private static final String SYSTEM_PROMPT = NbPreferences.forModule(AICompletionOptionsPanel.class).get("system_prompt_fim", "");

    public AiFimClient() {
        super(API_URL, API_KEY, MODEL, MAX_TOKENS, SYSTEM_PROMPT, null);
    }
        
    public String fetchSuggestion(String prompt, String toolChoice, String suffix) {
        
        try {
        // =========================================================================
        // TARGET SWITCH: Swap these strings when shifting from Cloud to Local Script
        // =========================================================================
        
        // Option A: Current Groq Cloud Testing Layout
        String targetModelName = "qwen/qwen3.8-27b";
        
        // Wrap the cursor data into a cloud instruction text block
        String payloadPrompt = "You are a code completion engine. Fill in the missing code lines.\n"
                             + "=== BEFORE CURSOR ===\n" + prompt + "\n"
                             + "=== AFTER CURSOR ===\n" + suffix + "\n\n"
                             + "Respond ONLY with raw missing code lines.";
        
        String jsonPayload = """
        {
          "model": "%s",
          "messages": [{"role": "user", "content": "%s"}],
          "temperature": 0.1
        }
        """.formatted(targetModelName, payloadPrompt.replace("\"", "\\\"").replace("\n", "\\n"));

        /*
        // Option B: Your Winter Local Script Layout (Port 8012 / 1.5B Base Model)
        String fimTargetUrl = "http://127.0.0";
        String authorizationHeader = "Bearer none"; // Local llama-server doesn't require keys
        
        // Construct the raw structural JSON payload llama-server's native FIM expects
        String jsonPayload = """
        {
          "input_prefix": "%s",
          "input_suffix": "%s",
          "n_predict": 64,
          "temperature": 0.0
        }
        """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"),
                      suffix.replace("\"", "\\\"").replace("\n", "\\n"));
        */
        // =========================================================================
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Using our custom tree parser skips neph1's static Choice.class object mappings entirely
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response.body());

                com.fasterxml.jackson.databind.JsonNode choices = rootNode.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String rawCode = choices.get(0).path("message").path("content").asText();

                    if (rawCode.startsWith("```")) {
                        rawCode = rawCode.replaceAll("```[a-zA-Z]*\\n", "").replace("```", "");
                    }
                    return rawCode.trim();
                }
            }

            // CRUCIAL: Remove the line that says "return responseParser.parseResponse(...)" 
            // down here, as that triggers the legacy class crash.
            return ""; 
//            return responseParser.parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "/* Error fetching AI completion */";
        }
    }
}
