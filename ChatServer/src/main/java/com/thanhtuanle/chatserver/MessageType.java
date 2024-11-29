package com.thanhtuanle.chatserver;

public enum MessageType {
    TEXT("text"),
    FILE("file"),
    GROUP_TEXT("group_text"),
    GROUP_FILE("group_file");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
