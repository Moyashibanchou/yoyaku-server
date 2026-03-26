package com.example.Artifact.service;

import org.springframework.stereotype.Service;

@Service
public class GeminiApiService {

    public String generateResponse(String prompt) throws Exception {
        // 例外発生テスト（モック）
        if (prompt.contains("エラー")) {
            throw new Exception("Simulated API Error");
        }

        // AIが回答できないテスト（モック）
        if (prompt.contains("わからない")) {
            return null;
        }

        return "Gemini（モック回答）: 「" + prompt + "」についてですね。";
    }
}
