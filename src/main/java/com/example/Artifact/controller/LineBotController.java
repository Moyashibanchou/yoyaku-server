package com.example.Artifact.controller;

import com.example.Artifact.model.UserInterest;
import com.example.Artifact.model.User;
import com.example.Artifact.model.Faq;
import com.example.Artifact.repository.UserInterestRepository;
import com.example.Artifact.repository.UserRepository;
import com.example.Artifact.repository.FaqRepository;
import com.example.Artifact.service.AiService;
import com.example.Artifact.service.ReservationService;
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
import java.util.concurrent.ConcurrentHashMap;

@LineMessageHandler
public class LineBotController {

    private final ReservationService reservationService;
    private final UserInterestRepository userInterestRepository;
    private final AiService aiService;
    private final UserRepository userRepository;
    private final FaqRepository faqRepository;
    private final ConcurrentHashMap<String, Boolean> aiModeUsers = new ConcurrentHashMap<>();

    @Autowired
    public LineBotController(ReservationService reservationService,
            UserInterestRepository userInterestRepository,
            AiService aiService,
            UserRepository userRepository,
            FaqRepository faqRepository) {
        this.reservationService = reservationService;
        this.userInterestRepository = userInterestRepository;
        this.aiService = aiService;
        this.userRepository = userRepository;
        this.faqRepository = faqRepository;
    }

    @EventMapping
    public Message handleTextMessageEvent(MessageEvent event) {
        if (event.message() instanceof TextMessageContent textMessageContent) {
            String text = textMessageContent.text();
            String userId = event.source().userId();

            // 既存の固定トリガー
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

            if ("AIに相談".equals(text)) {
                if (userId != null) {
                    aiModeUsers.put(userId, true);
                }
                return new TextMessage("美容師AIが何でもお答えします！質問をどうぞ✨\n（相談を終わる場合は「終了」と送ってください）");
            }

            if ("終了".equals(text) && Boolean.TRUE.equals(aiModeUsers.get(userId))) {
                aiModeUsers.remove(userId);
                return new TextMessage("AI相談モードを終了しました。");
            }

            // 有人対応トリガー
            if ("店員と話したい".equals(text)) {
                if (userId != null) {
                    User user = userRepository.findById(userId).orElse(new User(userId));
                    user.setWaitingForHuman(true);
                    userRepository.save(user);
                    // 管理者通知モック
                    System.out.println("【通知】ユーザー " + userId + " が店員との会話を希望しています。");
                }
                return new TextMessage("店員におつなぎします。少々お待ちください。");
            }

            // AI相談モードまたは汎用FAQ応答
            try {
                // 1. FAQハイブリッド検索（データベースからキーワード一致）
                List<Faq> faqs = faqRepository.findByKeywordContaining(text);
                if (faqs != null && !faqs.isEmpty()) {
                    return new TextMessage("【よくある質問】\n" + faqs.get(0).getAnswer());
                }

                // 2. Gemini連携による自然言語応答 (新しく作成した AiService を使用)
                String aiResponse = aiService.askAi(text);
                if (aiResponse == null || aiResponse.isBlank()) {
                    // 3. 有人モードフラグ更新 (AI回答不能の場合)
                    if (userId != null) {
                        User user = userRepository.findById(userId).orElse(new User(userId));
                        user.setWaitingForHuman(true);
                        userRepository.save(user);
                        System.out.println("【通知】ユーザー " + userId + " への自動回答ができなかったため、有人対応のリクエストが送信されました。");
                    }
                    return new TextMessage("うまく理解できませんでした…店員におつなぎしますので、そのまま少々お待ちください。");
                }

                return new TextMessage(aiResponse);

            } catch (Exception e) {
                // 4. 通信エラーやAPI制限の例外処理
                System.err.println("Gemini API処理中にエラーが発生しました: " + e.getMessage());
                e.printStackTrace();
                return new TextMessage("申し訳ありません、ただいまスタイリストが接客中で手が離せません。少し時間を置いてから再度話しかけてください🙏");
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
                    URI.create("https://images.unsplash.com/photo-1599305090598-fe179d501227?w=400&h=400&fit=crop"),
                    null,
                    "プレミアムリペアシャンプー",
                    "価格: ¥3,500",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://yoyaku-client.vercel.app"), null)));
            CarouselColumn col2 = new CarouselColumn(
                    URI.create("https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=400&h=400&fit=crop"),
                    null,
                    "高保湿ヘアオイル",
                    "価格: ¥2,800",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://yoyaku-client.vercel.app"), null)));
            CarouselColumn col3 = new CarouselColumn(
                    URI.create("https://images.unsplash.com/photo-1629367494173-c78a56567877?w=400&h=400&fit=crop"),
                    null,
                    "スカルプケアエッセンス",
                    "価格: ¥4,200",
                    null,
                    List.of(new URIAction("購入する", URI.create("https://yoyaku-client.vercel.app"), null)));

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
