package ru.ermakovis.simpleStorage.common;

public class FileChunkMessage extends Message {
    private final String fileName;
    private final byte[] chunk;
    private final int size;
    private final boolean isFinal;

    public FileChunkMessage(String fileName, byte[] chunk, int size, boolean isFinal) {
        this.fileName = fileName;
        this.chunk = chunk;
        this.size = size;
        this.isFinal = isFinal;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getChunk() {
        return chunk;
    }

    public int getSize() {
        return size;
    }

    public boolean isFinal() {
        return isFinal;
    }


}
