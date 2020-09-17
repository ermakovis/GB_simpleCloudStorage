package ru.ermakovis.simpleStorage.common;

import java.io.Serializable;

public class FileInfoMessage implements Serializable, Comparable<FileInfoMessage> {
    private final String name;
    private final long size;

    public FileInfoMessage(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public int compareTo(FileInfoMessage message) {
        return 0;
    }
}
