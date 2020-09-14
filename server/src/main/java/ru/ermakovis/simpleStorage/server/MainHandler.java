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
import java.util.stream.Stream;

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
            handleFileListMessage(ctx);
        }
    }

    private void handleFileListMessage(ChannelHandlerContext ctx) {
        logger.info("Getting local items");
        try (Stream<Path> walk = Files.walk(rootPath)) {
            List<String> items = walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            ctx.channel().writeAndFlush(items);
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException e ) {
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
