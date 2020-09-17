package ru.ermakovis.simpleStorage.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Client extends Application {
    private final Logger logger = LoggerFactory.getLogger(Client.class);

    private Network network;
    private Path localRoot;
    private Path remoteRoot = Path.of("");

    public void showError(String header, String text) {
        //TODO add styles
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(text);
        alert.showAndWait();
    }

    public boolean sendFile(String fileName) {
        logger.info("Sending file - " + fileName);
        Path filePath = localRoot.resolve(fileName);
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
                     new BufferedOutputStream(new FileOutputStream(localRoot.resolve(fileName).toFile()))) {
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

    public boolean removeLocalFile(String fileName) {
        return false;
    }

    public boolean removeRemoteFile(String fileName) {
        return false;
    }

    public void sendMessage(Message message) {
        try {
            network.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object receiveMessage() {
        try {
            return network.receiveMessage();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<FileInfoMessage> getLocalItems() {
        logger.info("Getting local items");
        try {
            return Files.list(localRoot)
                    .sorted()
                    .sorted((a, b) -> Boolean.compare(Files.isDirectory(b), Files.isDirectory(a)))
                    .map(this::getLocalItem)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public FileInfoMessage getLocalItem(Path path) {
        String fileName = localRoot.relativize(path).toString();
        try {
            long fileSize = Files.isDirectory(path) ? -1 : Files.size(path);
            return new FileInfoMessage(fileName, fileSize);
        } catch (IOException e) {
            return new FileInfoMessage(fileName, 0);
        }
    }

    public List<FileInfoMessage> getRemoteItems() {
        logger.info("Getting remote items");
        System.out.println(remoteRoot);
        sendMessage(new FileListMessage(remoteRoot.toString()));
        Object object = receiveMessage();
        return (List<FileInfoMessage>) object;
    }

    @Override
    public void start(Stage stage) throws Exception {
        localRoot = Path.of(System.getProperty("user.home"));
        network = new Network("localhost", 8189);
        Thread.sleep(1000);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.initController(this);
        stage.setResizable(false);
        stage.setTitle("SimpleCloudStorage");
        stage.setScene(new Scene(root));
        stage.show();
        stage.setOnCloseRequest((event) -> {
            network.stop();
            Platform.exit();
        });
    }

    public Path getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(Path localRoot) {
        this.localRoot = localRoot;
    }

    public Path getRemoteRoot() {
        return remoteRoot;
    }

    public void setRemoteRoot(Path remoteRoot) {
        this.remoteRoot = remoteRoot == null ? Path.of("") : remoteRoot;
    }


    public static void main(String[] args) {
        launch(args);
    }
}
