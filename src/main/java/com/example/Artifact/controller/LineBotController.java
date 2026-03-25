package com.example.Artifact.controller;

import com.example.Artifact.service.ReservationService;
import com.linecorp.bot.messaging.model.Message;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.PostbackEvent;
import org.springframework.beans.factory.annotation.Autowired;

@LineMessageHandler
public class LineBotController {

    private final ReservationService reservationService;

    @Autowired
    public LineBotController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @EventMapping
    public Message handlePostbackEvent(PostbackEvent event) {
        String data = event.postback().data();
        System.out.println("Received postback: data=" + data);

        if (data == null || data.isBlank()) {
            return new TextMessage("予約のキャンセルに失敗しました");
        }

        // expected: action=cancel&id={予約ID}
        String action = null;
        String id = null;
        for (String part : data.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if ("action".equals(kv[0])) action = kv[1];
            if ("id".equals(kv[0])) id = kv[1];
        }

        if (!"cancel".equals(action) || id == null || id.isBlank()) {
            return new TextMessage("予約のキャンセルに失敗しました");
        }

        boolean deleted = reservationService.deleteById(id);
        if (deleted) {
            return new TextMessage("予約のキャンセルが完了しました");
        }

        return new TextMessage("予約が見つかりませんでした");
    }
}

