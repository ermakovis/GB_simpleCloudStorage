package ru.ermakovis.simpleStorage.common;

public class ResultMessage extends Message {
    private final boolean isSuccessful;

    public ResultMessage(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}
