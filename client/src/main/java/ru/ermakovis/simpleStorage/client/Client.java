package ru.ermakovis.simpleStorage.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client extends Application {
    private final Logger logger = LoggerFactory.getLogger(Client.class);

    private Network network;
    private Path rootPath;

    public void showError(String header, String text) {
        //TODO add styles
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(text);
        alert.showAndWait();
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

    public List<String> getLocalItems() {
        logger.info("Getting local items");
        System.out.println(rootPath.toString());
        try (Stream<Path> walk = Files.walk(rootPath)) {
            return walk.filter(Files::isRegularFile)
                    .map(rootPath::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getRemoteItems() {
        logger.info("Getting remote items");
        sendMessage(new FileListMessage());
        Object object = receiveMessage();
        return (List<String>) object;
    }

    @Override
    public void start(Stage stage) throws Exception {
        rootPath = Path.of("C:", "admin", "server");
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

    public static void main(String[] args) {
        launch(args);
    }
}
