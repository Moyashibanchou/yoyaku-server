package com.example.Artifact.controller;

import com.example.Artifact.dto.ReservationRequest;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.PushMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "https://yoyaku-client.vercel.app")
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    private final MessagingApiClient messagingClient;

    @Value("${ADMIN_LINE_ID:}")
    private String adminLineId;

    // メモリ内の保存用リスト（一旦）
    private final List<ReservationRequest> reservations = new ArrayList<>();

    @Autowired
    public ReservationController(MessagingApiClient messagingClient) {
        this.messagingClient = messagingClient;
    }

    @PostMapping
    public String createReservation(@RequestBody ReservationRequest request) {
        // 受け取った内容をコンソールに出力
        System.out.println("受け取った予約データ: " + request);

        // 一旦メモリ内に保存
        reservations.add(request);

        // 管理者へのLINE通知
        if (adminLineId != null && !adminLineId.isEmpty()) {
            String messageText = String.format(
                    "【新規予約】\n" +
                            "お客様：%s 様\n" +
                            "電話：%s\n" +
                            "日時：%s %s\n" +
                            "担当：%s\n" +
                            "メニュー：%s\n" +
                            "クーポン：%s",
                    request.getUserName(),
                    request.getUserPhone(),
                    request.getReservationDate(),
                    request.getReservationTime(),
                    request.getAssistantName(),
                    request.getMenuName(),
                    request.getCouponName());

            try {
                PushMessageRequest pushMessage = new PushMessageRequest(
                        adminLineId,
                        List.of(new TextMessage(messageText)),
                        false,
                        null);
                messagingClient.pushMessage(UUID.randomUUID(), pushMessage).get();
                log.info("予約通知を管理者に送信しました。");
            } catch (Exception e) {
                log.error("予約通知の送信に失敗しました: " + e.getMessage(), e);
            }
        } else {
            log.warn("ADMIN_LINE_IDが設定されていないため、通知を送信しませんでした。");
        }

        return "Reservation created successfully";
    }
}
