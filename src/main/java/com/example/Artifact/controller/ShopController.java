package com.example.Artifact.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "https://yoyaku-client.vercel.app")
@RequestMapping("/api/shop")
public class ShopController {

    public record ShopItem(int id, String name, int price, String description) {
    }

    @GetMapping
    public List<ShopItem> getShopItems() {
        return List.of(
                new ShopItem(1, "プレミアムリペアシャンプー", 3500, "お悩み相談でおすすめされたアイテム"),
                new ShopItem(2, "高保湿ヘアオイル", 2800, "お悩み相談でおすすめされたアイテム"),
                new ShopItem(3, "スカルプケアエッセンス", 4200, "お悩み相談でおすすめされたアイテム"));
    }
}
