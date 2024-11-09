package com.lld.im.codec.pack.message;

import lombok.Data;

@Data
public class ChatMessageAck {
    private String messageId;

    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }
}
