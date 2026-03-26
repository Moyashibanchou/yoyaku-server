package com.example.Artifact.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiService {

  @Value("${gemini.api.key:}")
  private String geminiApiKey;

  private final RestTemplate restTemplate = new RestTemplate();

  @PostConstruct
  public void init() {
    String key = geminiApiKey;
    if (key == null || key.isBlank()) {
      key = System.getenv("GEMINI_API_KEY");
    }
    System.out.println("API Key Loaded: " + (key != null && !key.isBlank()));
  }

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

    // URLパラメータの末尾に直接キーを付与する方式（ヘッダー渡しを廃止）
    String url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key="
        + apiKey;

    String systemPrompt = "あなたは表参道の人気美容室『Hair Salon Demo』のプロ美容師です。お客様の髪の悩みに寄り添い、具体的で分かりやすい専門的なアドバイスを、明るく丁寧な口調で提供してください。";

    // JSONペイロードを手動で構築（Geminiが100%解釈できる最小限・最もシンプルな形）
    String jsonPayload = """
            {
              "systemInstruction": {
                "parts": [
                  { "text": "%s" }
                ]
              },
              "contents": [
                {
                  "parts": [
                    { "text": "%s" }
                  ]
                }
              ]
            }
        """.formatted(escapeJson(systemPrompt), escapeJson(question));

    // 必要なのは application/json の宣言のみ
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);

    System.out.println(
        "Gemini API Request URL: https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=HIDDEN");

    ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new Exception("Gemini API error: Status=" + response.getStatusCode() + " Body=" + response.getBody());
    }

    String responseBody = response.getBody();

    if (responseBody == null)
      return null;

    // 簡易的な文字列からの抽出
    int textIndex = responseBody.indexOf("\"text\": \"");
    if (textIndex != -1) {
      int startIndex = textIndex + 9;
      int endIndex = responseBody.indexOf("\"", startIndex);
      // エスケープフラグ等を修正
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
