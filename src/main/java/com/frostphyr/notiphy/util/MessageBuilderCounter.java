package com.frostphyr.notiphy.util;

import java.util.Map;

import com.google.firebase.messaging.Message;

public class MessageBuilderCounter {

    private Message.Builder builder;
    private int count;

    public MessageBuilderCounter(Message.Builder builder) {
        this.builder = builder;
    }

    public Message.Builder getBuilder() {
        return builder;
    }

    public MessageBuilderCounter putData(String key, String value) {
        builder.putData(key, value);
        count += key.length() + value.length();
        return this;
    }

    public MessageBuilderCounter putAllData(Map<String, String> map) {
        builder.putAllData(map);
        for (Map.Entry<String, String> e : map.entrySet()) {
            count += e.getKey().length() + e.getValue().length();
        }
        return this;
    }

    public int getCount() {
        return count;
    }

}
