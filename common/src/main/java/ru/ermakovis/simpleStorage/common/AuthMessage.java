package ru.ermakovis.simpleStorage.common;

public class AuthMessage extends Message {
    private final String userName;

    public AuthMessage(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
