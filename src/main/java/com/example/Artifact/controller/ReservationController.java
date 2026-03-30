package com.example.Artifact.controller;

import com.example.Artifact.dto.ReservationRequest;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController // 👈 コメントアウトを外して有効化しました
@CrossOrigin(origins = "https://yoyaku-client.vercel.app")
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);
    private final MessagingApiClient messagingClient;

    @Autowired
    public ReservationController(MessagingApiClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    @PostMapping
    public String createReservation(@RequestBody ReservationRequest request) {
        log.info("予約データ受信: {}", request);

        // 1. お客様のUserIDがあるかチェック
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            log.error("エラー: お客様のLINE UserIDが届いていません。");
            return "Error: userId is missing";
        }

        // 2. Flex Message（豪華な予約票）の組み立て
        // デザインパーツの作成
        FlexBox body = FlexBox.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(List.of(
                        FlexText.builder().text("【予約内容の確認】").weight(FlexText.FlexTextWeight.BOLD).size("xl").build(),
                        FlexBox.builder().layout(FlexLayout.VERTICAL).margin("lg").spacing("sm").contents(List.of(
                                FlexText.builder().text("メニュー： " + request.getMenuName()).build(),
                                FlexText.builder()
                                        .text("日時： " + request.getReservationDate() + " "
                                                + request.getReservationTime())
                                        .build(),
                                FlexText.builder().text("担当： " + request.getAssistantName()).build(),
                                FlexText.builder().text("クーポン： " + request.getCouponName()).build())).build()))
                .build();

        FlexBox footer = FlexBox.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(List.of(
                        FlexButton.builder()
                                .style(FlexButton.FlexButtonStyle.PRIMARY)
                                .color("#48BB78")
                                .action(new UriAction("予約をキャンセル", "https://line.me/R/oaMessage/@YOUR_BOT_ID/", null))
                                .build()))
                .build();

        FlexBubble bubble = FlexBubble.builder()
                .body(body)
                .footer(footer)
                .build();

        FlexMessage flexMessage = new FlexMessage("予約内容の確認", bubble);

        // 3. お客様(request.getUserId) 宛にプッシュ送信
        try {
            PushMessageRequest pushMessageRequest = new PushMessageRequest(
                    request.getUserId(),
                    List.of(flexMessage),
                    false,
                    null);

            messagingClient.pushMessage(UUID.randomUUID(), pushMessageRequest).get();
            log.info("お客様（{}）へ予約票を送信しました。", request.getUserId());

        } catch (Exception e) {
            log.error("LINE送信失敗: " + e.getMessage(), e);
            return "Error: Failed to send LINE message";
        }

        return "Reservation created and message sent successfully";
    }
}