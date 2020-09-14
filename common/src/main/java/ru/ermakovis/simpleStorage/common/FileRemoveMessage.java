package ru.ermakovis.simpleStorage.common;

public class FileRemoveMessage extends Message {
    private final String fileName;

    public FileRemoveMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
