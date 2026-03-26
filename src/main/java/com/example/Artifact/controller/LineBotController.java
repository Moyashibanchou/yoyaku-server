package com.example.Artifact.controller;

import com.example.Artifact.model.UserInterest;
import com.example.Artifact.repository.UserInterestRepository;
import com.example.Artifact.service.ReservationService;
import com.linecorp.bot.messaging.model.Action;
import com.linecorp.bot.messaging.model.ButtonsTemplate;
import com.linecorp.bot.messaging.model.CarouselColumn;
import com.linecorp.bot.messaging.model.CarouselTemplate;
import com.linecorp.bot.messaging.model.Message;
import com.linecorp.bot.messaging.model.PostbackAction;
import com.linecorp.bot.messaging.model.TemplateMessage;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.messaging.model.URIAction;
import com.linecorp.bot.spring.boot.handler.annotation.EventMapping;
import com.linecorp.bot.spring.boot.handler.annotation.LineMessageHandler;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.PostbackEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;

@LineMessageHandler
public class LineBotController {

    private final ReservationService reservationService;
    private final UserInterestRepository userInterestRepository;

    @Autowired
    public LineBotController(ReservationService reservationService, UserInterestRepository userInterestRepository) {
        this.reservationService = reservationService;
        this.userInterestRepository = userInterestRepository;
    }

    @EventMapping
    public Message handleTextMessageEvent(MessageEvent event) {
        if (event.message() instanceof TextMessageContent textMessageContent) {
            String text = textMessageContent.text();
            if ("お悩み相談".equals(text)) {
                return new TemplateMessage(
                        "ヘアケア相談",
                        new ButtonsTemplate(
                                null,
                                null,
                                null,
                                null,
                                "ヘアケア相談",
                                "現在の髪のお悩みは？",
                                null,
                                List.of(
                                        new PostbackAction("パサつき", "action=tag&cat=dry", null, null, null, null),
                                        new PostbackAction("ボリューム", "action=tag&cat=volume", null, null, null, null),
                                        new PostbackAction("ダメージ", "action=tag&cat=damage", null, null, null, null))));
            }
        }
        return null;
    }

    @EventMapping
    public Message handlePostbackEvent(PostbackEvent event) {
        String data = event.postback().data();
        System.out.println("Received postback: data=" + data);

        if (data == null || data.isBlank()) {
            return new TextMessage("予約のキャンセルに失敗しました");
        }

        String action = null;
        String id = null;
        String cat = null;
        for (String part : data.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2)
                continue;
            if ("action".equals(kv[0]))
                action = kv[1];
            if ("id".equals(kv[0]))
                id = kv[1];
            if ("cat".equals(kv[0]))
                cat = kv[1];
        }

        if ("tag".equals(action) && cat != null) {
            String userId = event.source().userId();
            if (userId != null) {
                UserInterest userInterest = userInterestRepository.findById(userId)
                        .orElse(new UserInterest(userId, cat));
                userInterest.setCategory(cat);
                userInterestRepository.save(userInterest);
            }

            CarouselColumn col1 = new CarouselColumn(
                    URI.create("https://example.com/item1.jpg"),
                    null,
                    "商品おすすめ1 (" + cat + ")",
                    "価格: ¥1,500",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://base.in/item1"), null)));
            CarouselColumn col2 = new CarouselColumn(
                    URI.create("https://example.com/item2.jpg"),
                    null,
                    "商品おすすめ2 (" + cat + ")",
                    "価格: ¥2,000",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://base.in/item2"), null)));
            CarouselColumn col3 = new CarouselColumn(
                    URI.create("https://example.com/item3.jpg"),
                    null,
                    "商品おすすめ3 (" + cat + ")",
                    "価格: ¥2,500",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://base.in/item3"), null)));

            return new TemplateMessage(
                    "おすすめ商品",
                    new CarouselTemplate(
                            List.of(col1, col2, col3),
                            null,
                            null));
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
