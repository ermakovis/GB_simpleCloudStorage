package ru.ermakovis.simpleStorage.common;

public class FileDownloadMessage extends Message {
    private final String fileName;

    public FileDownloadMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
