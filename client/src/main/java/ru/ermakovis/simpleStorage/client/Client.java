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
import java.util.stream.Stream;

public class Client extends Application {
    private final Logger logger = LoggerFactory.getLogger(Client.class);

    private Network network;
    private Path localRoot;
    private Path remoteRoot = Path.of("");

    public static void main(String[] args) {
        launch(args);
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

    public void showError(String header, String text) {
        //TODO add styles
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(text);
        alert.showAndWait();
    }

    public void sendFiles(String fileName) {
        logger.info("Start of sending routine");
        if (!Files.isDirectory(localRoot.resolve(fileName))) {
            sendFile(fileName);
            return;
        }

        try (Stream<Path> walk = Files.walk(localRoot.resolve(fileName))) {
            walk.filter(Files::isRegularFile)
                .forEach(path -> sendFile(localRoot.relativize(path).toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO handle errors
    public void sendFile(String fileName) {
        logger.info("Sending file - " + fileName);
        Path filePath = localRoot.resolve(fileName);
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            network.sendMessage(new FileUploadMessage(fileName));
            if (!((ResultMessage) network.receiveMessage()).isSuccessful()) {
                return;
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
        }
    }

    public void receiveFiles(String fileName) {
        logger.info("Sending receive list request - " + fileName);
        try {
            sendMessage(new FileDownloadListMessage(remoteRoot.resolve(fileName).toString()));
            List<String> fileList = (List<String>) network.receiveMessage();
            logger.info("Received list of size - " + fileList.size());
            for (String file : fileList) {
                receiveFile(file);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //TODO handle errors
    public void receiveFile(String fileName) {
        logger.info("Receiving file " + fileName);
        Path filePath = localRoot.resolve(fileName);
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            network.sendMessage(new FileDownloadMessage(fileName));
            ResultMessage resultMessage = (ResultMessage) network.receiveMessage();
            if (!resultMessage.isSuccessful()) {
                return;
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
        }
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



    public Path getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(Path localRoot) {
        this.localRoot = localRoot;
    }

    public Path getRemoteRoot() {
        return remoteRoot;
    }

    //почему-то Path.of("") в качестве аргумента он не принимает
    public void setRemoteRoot(Path remoteRoot) {
        this.remoteRoot = remoteRoot == null ? Path.of("") : remoteRoot;
    }



}
