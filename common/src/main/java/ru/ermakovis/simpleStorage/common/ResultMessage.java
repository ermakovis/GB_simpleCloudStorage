package ru.ermakovis.simpleStorage.common;

public class ResultMessage extends Message {
    private final Throwable error;

    public ResultMessage(Throwable error) {
        this.error = error;
    }

    public ResultMessage() {
        this.error = null;
    }

    public boolean isSuccessful() {
        return error == null;
    }

    public Throwable getError() {
        return error;
    }
}
