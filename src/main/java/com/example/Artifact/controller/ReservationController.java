package com.example.Artifact.controller;

import com.example.Artifact.dto.ReservationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final RestTemplate restTemplate;

    @Value("${line.bot.token}")
    private String lineBotToken;

    public ReservationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("API is running!");
    }

    @PostMapping
    public ResponseEntity<String> createReservation(@RequestBody ReservationRequest request) {
        System.out.println("Received reservation request: userId=" + request.getUserId()
                + ", menu=" + request.getMenu()
                + ", dateTime=" + request.getDateTime()
                + ", staff=" + request.getStaff()
                + ", useCoupon=" + request.getUseCoupon());

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body("userId is required");
        }

        if (lineBotToken == null || lineBotToken.isBlank()) {
            return ResponseEntity.status(500).body("line.bot.token is not configured");
        }

        String displayMenu = (request.getMenu() != null && !request.getMenu().isBlank())
                ? request.getMenu()
                : "（未指定）";
        String displayDateTime = (request.getDateTime() != null && !request.getDateTime().isBlank())
                ? request.getDateTime()
                : "（未指定）";
        String displayStaff = (request.getStaff() != null && !request.getStaff().isBlank())
                ? request.getStaff()
                : "（未指定）";
        String couponLabel = request.getUseCoupon() ? "あり" : "なし";

        String messageText = String.join("\n",
                "【予約内容】",
                "メニュー：" + displayMenu,
                "日時：" + displayDateTime,
                "担当者：" + displayStaff,
                "クーポン利用：" + couponLabel);

        // LINE Messaging API へのリクエストボディ作成
        Map<String, Object> body = new HashMap<>();
        body.put("to", request.getUserId());
        
        Map<String, String> message = new HashMap<>();
        message.put("type", "text");
        message.put("text", messageText);
        
        body.put("messages", Collections.singletonList(message));

        // ヘッダーの設定
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(lineBotToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // LINE API を叩く
        String url = "https://api.line.me/v2/bot/message/push";
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("LINE push success: status=" + response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body("LINEメッセージ送信成功: " + response.getBody());
        } catch (Exception e) {
            System.out.println("LINE push failed: " + e.getMessage());
            return ResponseEntity.status(500).body("LINEメッセージ送信失敗: " + e.getMessage());
        }
    }
}
