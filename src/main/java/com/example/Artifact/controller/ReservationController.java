package com.example.Artifact.controller;

import com.example.Artifact.dto.ReservationRequest;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
        FlexText title = new FlexText(
                null,
                "【予約内容の確認】",
                "xl",
                null,
                null,
                null,
                FlexText.Weight.BOLD,
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        FlexBox detailBox = createVerticalBox(
                List.<FlexComponent>of(
                        new FlexText(null, "メニュー： " + request.getMenuName(), null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null, null, null, null),
                        new FlexText(null, "日時： " + request.getReservationDate() + " " + request.getReservationTime(), null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null, null, null, null),
                        new FlexText(null, "担当： " + request.getAssistantName(), null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null, null, null, null),
                        new FlexText(null, "クーポン： " + request.getCouponName(), null, null, null, null, null, null, null, true, null, null, null, null, null, null, null, null, null, null, null, null)
                ),
                "sm",
                "lg");

        FlexBox body = createVerticalBox(
                List.<FlexComponent>of(title, detailBox),
                null,
                null);

        URIAction cancelAction = new URIAction(
                "予約をキャンセル",
                URI.create("https://line.me/R/oaMessage/@YOUR_BOT_ID/"),
                null);

        FlexButton cancelButton = new FlexButton(
                null,
                "#48BB78",
                FlexButton.Style.PRIMARY,
                cancelAction,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        FlexBox footer = createVerticalBox(
                List.<FlexComponent>of(cancelButton),
                null,
                null);

        FlexBubble bubble = new FlexBubble(
                null,
                null,
                null,
                null,
                body,
                footer,
                null,
                null);

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
            log.error("LINE: " + e.getMessage(), e);
            return "Error: Failed to send LINE message";
        }

        return "Reservation created and message sent successfully";
    }

    private static FlexBox createVerticalBox(List<FlexComponent> contents, String spacing, String margin) {
        return new FlexBox(
                FlexBox.Layout.VERTICAL,
                null,
                contents,
                spacing,
                margin,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}