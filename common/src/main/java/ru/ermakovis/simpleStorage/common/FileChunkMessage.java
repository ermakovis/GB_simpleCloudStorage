package ru.ermakovis.simpleStorage.common;

public class FileChunkMessage extends Message {
    private final byte[] chunk;
    private final int size;
    private final boolean isFinal;

    public FileChunkMessage(byte[] chunk, int size, boolean isFinal) {
        this.chunk = chunk;
        this.size = size;
        this.isFinal = isFinal;
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
