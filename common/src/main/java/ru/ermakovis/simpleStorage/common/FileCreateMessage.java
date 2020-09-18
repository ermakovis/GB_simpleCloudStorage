package ru.ermakovis.simpleStorage.common;

public class FileCreateMessage extends Message {
    private final String fileName;

    public FileCreateMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
