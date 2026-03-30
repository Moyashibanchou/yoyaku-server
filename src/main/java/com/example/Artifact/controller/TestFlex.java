package com.example.Artifact.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.messaging.model.FlexMessage;

public class TestFlex {
    public void test() throws Exception {
        String json = "{\"type\": \"flex\", \"altText\": \"予約票\", \"contents\": { \"type\": \"bubble\", \"body\": { \"type\": \"box\", \"layout\": \"vertical\", \"contents\": [ { \"type\": \"text\", \"text\": \"hello\" } ] } } }";
        FlexMessage msg = new ObjectMapper().readValue(json, FlexMessage.class);
    }
}
