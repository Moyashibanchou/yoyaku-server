package com.example.Artifact.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class AiService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    /**
     * 外部のAI API（Gemini）と通信して相談の回答を生成するメソッド
     * 
     * @param question ユーザーからの質問
     * @return AIからの返答内容
     * @throws Exception API呼び出し失敗時
     */
    public String askAi(String question) throws Exception {
        String apiKey = geminiApiKey;
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("GEMINI_API_KEY");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new Exception("GEMINI_API_KEY is not set");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                + apiKey;

        String systemPrompt = "あなたは表参道の人気美容室『Hair Salon Demo』のプロ美容師です。お客様の髪の悩みに寄り添い、具体的で分かりやすい専門的なアドバイスを、明るく丁寧な口調で提供してください。";

        // JSONペイロードを手動で構築（簡易版）
        String jsonPayload = """
                    {
                      "systemInstruction": {
                        "parts": [
                          { "text": "%s" }
                        ]
                      },
                      "contents": [
                        {
                          "role": "user",
                          "parts": [
                            { "text": "%s" }
                          ]
                        }
                      ]
                    }
                """.formatted(escapeJson(systemPrompt), escapeJson(question));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Gemini API error: " + response.statusCode() + " " + response.body());
        }

        String responseBody = response.body();

        // 簡易的な文字列からの抽出 (Jackson依存をなくすため)
        // 本来は正規表現やJSONパーサーを使いますが、Geminiの構造上 "text": "..." が含まれます
        int textIndex = responseBody.indexOf("\"text\": \"");
        if (textIndex != -1) {
            int startIndex = textIndex + 9;
            int endIndex = responseBody.indexOf("\"", startIndex);
            // エスケープされた改行などを戻す
            String extracted = responseBody.substring(startIndex, endIndex);
            return extracted.replace("\\n", "\n").replace("\\\"", "\"");
        }

        return null;
    }

    private String escapeJson(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
