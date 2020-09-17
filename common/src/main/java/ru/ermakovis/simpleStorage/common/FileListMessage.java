package ru.ermakovis.simpleStorage.common;

public class FileListMessage extends Message {
    private final String root;

    public FileListMessage(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }
}
