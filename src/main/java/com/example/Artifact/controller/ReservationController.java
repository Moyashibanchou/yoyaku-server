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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    private final RestTemplate restTemplate;
    private final com.example.Artifact.service.ReservationService reservationService;

    @Value("${line.bot.token}")
    private String lineBotToken;

    public ReservationController(RestTemplate restTemplate,
                                  com.example.Artifact.service.ReservationService reservationService) {
        this.restTemplate = restTemplate;
        this.reservationService = reservationService;
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

        String reservationId = reservationService.createReservation(request);

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

        // Flex Message（予約内容 + 末尾に「予約をキャンセル」ボタン）
        Map<String, Object> bodyBox = new HashMap<>();
        bodyBox.put("type", "box");
        bodyBox.put("layout", "vertical");
        bodyBox.put("contents", List.of(
                Map.of("type", "text", "text", "【予約内容】", "weight", "bold", "size", "md"),
                Map.of("type", "text", "text", "メニュー：" + displayMenu, "size", "sm"),
                Map.of("type", "text", "text", "日時：" + displayDateTime, "size", "sm"),
                Map.of("type", "text", "text", "担当者：" + displayStaff, "size", "sm"),
                Map.of("type", "text", "text", "クーポン利用：" + couponLabel, "size", "sm")
        ));

        Map<String, Object> cancelPostback = Map.of(
                "type", "postback",
                "label", "予約をキャンセル",
                "data", "action=cancel&id=" + reservationId
        );

        Map<String, Object> cancelButton = Map.of(
                "type", "button",
                "style", "primary",
                "action", cancelPostback
        );

        Map<String, Object> footerBox = new HashMap<>();
        footerBox.put("type", "box");
        footerBox.put("layout", "vertical");
        footerBox.put("contents", List.of(cancelButton));

        Map<String, Object> bubble = new HashMap<>();
        bubble.put("type", "bubble");
        bubble.put("body", bodyBox);
        bubble.put("footer", footerBox);

        // LINE Messaging API へのリクエストボディ作成
        Map<String, Object> body = new HashMap<>();
        body.put("to", request.getUserId());
        
        Map<String, Object> message = new HashMap<>();
        message.put("type", "flex");
        message.put("altText", "予約が完了しました");
        message.put("contents", bubble);
        
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
