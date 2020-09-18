package ru.ermakovis.simpleStorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
            logger.info("FileListMessage received");
            handleFileListMessage((FileListMessage) msg, ctx);
        } else if (msg instanceof FileDownloadListMessage) {
            logger.info("FileDownloadList received");
            handleFileDownloadListMessage((FileDownloadListMessage) msg, ctx);
        } else if (msg instanceof FileRemoveMessage) {
            logger.info("FileRemoveMessage received");
            handleFileRemoveMessage((FileRemoveMessage) msg);
        }
    }

    private void handleFileRemoveMessage(FileRemoveMessage message) {
        Path filePath = rootPath.resolve(message.getFileName());
        if (!Files.isDirectory(filePath)) {
            removeLocalFile(filePath);
            return;
        }
        logger.info("Removing local folder");
        try {
            Files.walk(filePath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::removeLocalFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeLocalFile(Path path) {
        try {
            Files.delete(path);
            logger.info("Local file removed - " + path);
        } catch (IOException ignored) {
        }
    }

    private void handleFileDownloadListMessage(FileDownloadListMessage message, ChannelHandlerContext ctx) {
        Path root = rootPath.resolve(message.getFileName());
        if (!Files.isDirectory(root)) {
            ctx.channel().writeAndFlush(List.of(message.getFileName()));
            return;
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<String> list = walk.filter(Files::isRegularFile)
                    .map(rootPath::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            ctx.channel().writeAndFlush(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileDownloadMessage(FileDownloadMessage message, ChannelHandlerContext ctx) {
        Path filePath = rootPath.resolve(message.getFileName());
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
        } catch (IOException e) {
            ctx.channel().writeAndFlush(new ResultMessage(e));
        }

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            String fileName = message.getFileName();
            ctx.channel().writeAndFlush(new ResultMessage());
            int bytesRead;
            byte[] buf = new byte[65535];
            while ((bytesRead = inputStream.read(buf)) != -1) {
                FileChunkMessage chunkMessage =
                        new FileChunkMessage(fileName, buf, bytesRead, inputStream.available() == 0);
                ctx.channel().writeAndFlush(chunkMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileUploadMessage(FileUploadMessage message, ChannelHandlerContext ctx) {
        try {
            String fileName = message.getFileName();
            Path filePath = rootPath.resolve(fileName);
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            outputStream = new BufferedOutputStream(new FileOutputStream(filePath.toFile()));
            ctx.channel().writeAndFlush(new ResultMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
            ctx.channel().writeAndFlush(new ResultMessage(e));
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
            logger.error(e.getMessage());
        }
    }

    private void handleFileListMessage(FileListMessage message, ChannelHandlerContext ctx) {
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
            logger.error(e.getMessage());
        }
    }

    private FileInfoMessage getLocalItem(Path path, Path currentRootPath) {
        String fileName = currentRootPath.relativize(path).toString();
        try {
            long fileSize = Files.isDirectory(path) ? -1 : Files.size(path);
            return new FileInfoMessage(fileName, fileSize);
        } catch (IOException e) {
            return new FileInfoMessage(fileName, 0);
        }
    }

}
