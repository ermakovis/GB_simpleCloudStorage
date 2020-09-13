package ru.ermakovis.simpleStorage.common;

public class FileUploadMessage extends Message {
    private final String fileName;

    public FileUploadMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
