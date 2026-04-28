package com.joelmir.chatwifi;

public class MessageModel {

    private final String text;
    private final String sender;
    private final boolean isMe;
   
    public MessageModel(String text, String sender, boolean isMe) {
        this.text = text;
        this.sender = sender;
        this.isMe = isMe;
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }

    public boolean isMe() {
        return isMe;
    }
}