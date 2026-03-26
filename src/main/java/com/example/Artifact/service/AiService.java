package com.example.Artifact.service;

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

    // 接続先URLの確認: エンドポイント (キーはヘッダーで送る)
    String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    String systemPrompt = "あなたは表参道の人気美容室『Hair Salon Demo』のプロ美容師です。お客様の髪の悩みに寄り添い、具体的で分かりやすい専門的なアドバイスを、明るく丁寧な口調で提供してください。";

    // JSONペイロードを手動で構築（内容はGeminiが期待する current form に準拠）
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
              ],
              "generationConfig": {
                "temperature": 0.7
              }
            }
        """.formatted(escapeJson(systemPrompt), escapeJson(question));

    // RestTemplateの設定: ヘッダーにAPIキーを設定
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-goog-api-key", apiKey);

    HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);

    System.out.println("Gemini API Requst URI: " + url);

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
