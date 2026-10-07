package com.filevault.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class LlmService {

    @Value("${gemini.api-key:${GEMINI_API_KEY:${LLM_API_KEY:}}}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateResponse(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null; // Signals LLM key is not configured, fall back to DB/RAG structured response
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey.trim();

            String combinedPrompt = systemPrompt + "\n\nUser Question:\n" + userMessage;
            String escapedPrompt = escapeJson(combinedPrompt);

            String requestBody = "{\n" +
                    "  \"contents\": [{\n" +
                    "    \"parts\": [{\"text\": \"" + escapedPrompt + "\"}]\n" +
                    "  }]\n" +
                    "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractTextFromGeminiResponse(response.body());
            } else {
                System.err.println("Gemini API Error (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed calling Gemini API: " + e.getMessage());
        }

        return null;
    }

    private String extractTextFromGeminiResponse(String json) {
        try {
            int textIdx = json.indexOf("\"text\": \"");
            if (textIdx != -1) {
                int start = textIdx + 9;
                StringBuilder sb = new StringBuilder();
                boolean escaped = false;
                for (int i = start; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (escaped) {
                        if (c == 'n') sb.append('\n');
                        else if (c == 'r') sb.append('\r');
                        else if (c == 't') sb.append('\t');
                        else sb.append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("Failed parsing Gemini JSON response: " + e.getMessage());
        }
        return json;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
