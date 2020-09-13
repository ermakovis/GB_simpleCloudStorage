package ru.ermakovis.simpleStorage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Path;
import java.sql.ResultSet;

public class Client {
    private final Logger logger = LoggerFactory.getLogger(Client.class);
    private final Path rootPath;
    private final Network network = new Network("localhost", 8189);

    public Client(String rootName) {
        rootPath = Path.of(rootName);
        sendFile("test.iso");
        sendFile("test.txt");
        receiveFile("test2.iso");
        System.out.println("Network started");
    }

    public boolean sendFile(String fileName) {
        logger.info("Sending file - " + fileName);
        Path filePath = rootPath.resolve(fileName);
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            network.sendMessage(new FileUploadMessage(fileName));
            if (!((ResultMessage) network.receiveMessage()).isSuccessful()) {
                return false;
            }
            int bytesRead = 0;
            byte[] buf = new byte[65535];
            while ((bytesRead = stream.read(buf)) != -1) {
                FileChunkMessage message =
                        new FileChunkMessage(fileName, buf, bytesRead, stream.available() == 0);
                sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean receiveFile(String fileName) {
        logger.info("Sending receive request - " + fileName);
        try (BufferedOutputStream stream =
                     new BufferedOutputStream(new FileOutputStream(rootPath.resolve(fileName).toFile()))) {
            sendMessage(new FileDownloadMessage(fileName));
            if (!((ResultMessage) network.receiveMessage()).isSuccessful()) {
                return false;
            }
            while (true) {
                FileChunkMessage chunkMessage = (FileChunkMessage) network.receiveMessage();
                stream.write(chunkMessage.getChunk(), 0, chunkMessage.getSize());
                if (chunkMessage.isFinal()) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void sendMessage(Message message) {
        try {
            network.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String rootName = Path.of("C:", "admin", "client").toString();
        new Client(rootName);
    }
}
