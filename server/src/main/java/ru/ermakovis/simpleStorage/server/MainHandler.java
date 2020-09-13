package ru.ermakovis.simpleStorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.FileChunkMessage;
import ru.ermakovis.simpleStorage.common.FileDownloadMessage;
import ru.ermakovis.simpleStorage.common.FileUploadMessage;
import ru.ermakovis.simpleStorage.common.ResultMessage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(MainHandler.class);
    private final Path rootPath;

    private BufferedOutputStream outputStream = null;

    public MainHandler(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FileUploadMessage) {
            logger.info("FileUploadMessage received");
            boolean result = setupFileReceive(((FileUploadMessage) msg).getFileName());
            ctx.channel().writeAndFlush(new ResultMessage(result));
        } else if (msg instanceof FileChunkMessage) {
            FileChunkMessage chunkMessage = (FileChunkMessage) msg;
            outputStream.write(chunkMessage.getChunk(), 0, chunkMessage.getSize());
            if (chunkMessage.isFinal()) {
                logger.info("FileReceive finished");
                outputStream.close();
            }
        } else if (msg instanceof FileDownloadMessage) {
            FileDownloadMessage message = (FileDownloadMessage) msg;
            String fileName = message.getFileName();
            Path filePath = rootPath.resolve(fileName);
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            ctx.channel().writeAndFlush(new ResultMessage(true));
            int bytesRead = 0;
            byte[] buf = new byte[65535];
            while ((bytesRead = inputStream.read(buf)) != -1) {
                FileChunkMessage chunkMessage =
                        new FileChunkMessage(fileName, buf, bytesRead, inputStream.available() == 0);
                ctx.channel().writeAndFlush(chunkMessage);
            }
        }
    }

    private boolean setupFileReceive(String fileName) {
        try {
            Path filePath = rootPath.resolve(fileName);
            if (Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
