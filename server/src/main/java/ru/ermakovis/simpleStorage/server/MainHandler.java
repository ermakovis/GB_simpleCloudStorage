package ru.ermakovis.simpleStorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(MainHandler.class);
    private final Path rootPath;

    private BufferedOutputStream outputStream = null;

    public MainHandler(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FileUploadMessage) {
            logger.info("FileUploadMessage received");
            handleFileUploadMessage((FileUploadMessage) msg, ctx);
        } else if (msg instanceof FileChunkMessage) {
            handleFileChunkMessage((FileChunkMessage) msg);
        } else if (msg instanceof FileDownloadMessage) {
            logger.info("FileDownloadMessage received");
            handleFileDownloadMessage((FileDownloadMessage) msg, ctx);
        } else if (msg instanceof FileListMessage) {
            logger.info("FileListMessage received");
            handleFileListMessage((FileListMessage) msg, ctx);
        }
    }

    private void handleFileListMessage(FileListMessage message, ChannelHandlerContext ctx) {
        logger.info("Getting local items");
        Path currentRootPath = rootPath.resolve(message.getRoot());
        try {
            List<FileInfoMessage> list =
                    Files.list(currentRootPath)
                            .sorted()
                            .sorted((a, b) -> Boolean.compare(Files.isDirectory(b), Files.isDirectory(a)))
                            .map(a -> getLocalItem(a, currentRootPath))
                            .collect(Collectors.toList());
            ctx.channel().writeAndFlush(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileInfoMessage getLocalItem(Path path, Path currentRootPath) {
        String fileName = currentRootPath.relativize(path).toString();
        try {
            long fileSize = Files.isDirectory(path) ? -1 : Files.size(path);
            System.out.println(fileName + " " + fileSize);
            return new FileInfoMessage(fileName, fileSize);
        } catch (IOException e) {
            return new FileInfoMessage(fileName, 0);
        }
    }

    private void handleFileDownloadMessage(FileDownloadMessage message, ChannelHandlerContext ctx) {
        try {
            String fileName = message.getFileName();
            Path filePath = rootPath.resolve(fileName);
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()));
            ctx.channel().writeAndFlush(new ResultMessage(true));
            int bytesRead;
            byte[] buf = new byte[65535];
            while ((bytesRead = inputStream.read(buf)) != -1) {
                FileChunkMessage chunkMessage =
                        new FileChunkMessage(fileName, buf, bytesRead, inputStream.available() == 0);
                ctx.channel().writeAndFlush(chunkMessage);
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileUploadMessage(FileUploadMessage message, ChannelHandlerContext ctx) {
        try {
            String fileName = message.getFileName();
            Path filePath = rootPath.resolve(fileName);
            if (Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
            ctx.channel().writeAndFlush(new ResultMessage(true));
        } catch (IOException e) {
            e.printStackTrace();
            ctx.channel().writeAndFlush(new ResultMessage(false));
        }
    }

    private void handleFileChunkMessage(FileChunkMessage message) {
        try {
            outputStream.write(message.getChunk(), 0, message.getSize());
            if (message.isFinal()) {
                logger.info("FileReceive finished");
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
