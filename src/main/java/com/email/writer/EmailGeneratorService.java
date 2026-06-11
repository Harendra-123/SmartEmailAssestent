package com.email.writer;

import org.springframework.beans.factory.annotation.Value; // ✅ correct
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

//import com.fasterxml.jackson.databind.JsonNode; // ✅ correct
//import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;
    private final String apiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder,
                                 @Value("${gemini.api.url}") String baseUrl,
                                 @Value("${gemini.api.key}") String geminiApiKey) {

        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = geminiApiKey;
    }

    public String generateEmailRaply(EmailRequest emailRequest) {

        String promt = buildPromt(emailRequest);

        String requestBody = String.format("""
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }""", promt);

        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-2.5-flash:generateContent")
                        .build())
                .header("x-goog-api-key", apiKey)
                .header("Content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);

            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPromt(EmailRequest emailRequest) {
        StringBuilder promt = new StringBuilder();

        promt.append("Generate a professional email reply for the following email: ");

        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            promt.append("Use a ").append(emailRequest.getTone()).append(" tone. ");
        }

        promt.append("Original email:\n")
                .append(emailRequest.getEmailContent());

        return promt.toString();
    }

//    public String generateEmailRaply(EmailRequest emailRequest) {
//        return "harendra";
//    }

}