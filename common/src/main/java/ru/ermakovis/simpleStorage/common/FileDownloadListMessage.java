package ru.ermakovis.simpleStorage.common;

public class FileDownloadListMessage extends Message {
    private final String fileName;

    public FileDownloadListMessage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
