package ru.ermakovis.simpleStorage.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client extends Application {
    private final Logger logger = LoggerFactory.getLogger(Client.class);
    private Network network;
    private Path remoteRoot = Path.of("");
    private Path localRoot = Path.of(System.getProperty("user.home"));

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        network = getNetwork();


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

    public void showError(Throwable throwable) {
        logger.error(throwable.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error message");
        alert.setHeaderText("Exception was thrown");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

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
        } catch (Exception e) {
            showError(e);
        }
    }

    public void sendFile(String fileName) {
        logger.info("Sending file - " + fileName);
        Path filePath = localRoot.resolve(fileName);
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            network.sendMessage(new FileUploadMessage(fileName));
            ResultMessage resultMessage = (ResultMessage) network.receiveMessage();
            if (!resultMessage.isSuccessful()) {
                showError(resultMessage.getError());
            }
            int bytesRead;
            byte[] buf = new byte[65535];
            while ((bytesRead = stream.read(buf)) != -1) {
                FileChunkMessage message =
                        new FileChunkMessage(buf, bytesRead, stream.available() == 0);
                sendMessage(message);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void receiveFiles(String fileName) {
        logger.info("Sending receive list request - " + fileName);
        try {
            sendMessage(new FileDownloadListMessage(remoteRoot.resolve(fileName).toString()));
            List<String> fileList = (List<String>) network.receiveMessage();
            logger.info("Received list of size - " + fileList.size());
            for (String file : fileList) {
                receiveFile(file);
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    public void receiveFile(String fileName) {
        logger.info("Receiving file " + fileName);
        Path filePath = localRoot.resolve(fileName);
        try {
            if (!Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
        } catch (Exception e) {
            showError(e);
            return;
        }

        try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            network.sendMessage(new FileDownloadMessage(fileName));
            ResultMessage resultMessage = (ResultMessage) network.receiveMessage();
            if (resultMessage.isSuccessful()) {
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
            showError(e);
        }
    }

    public void createLocalFolder(String fileName) {
        try {
            Files.createDirectory(localRoot.resolve(fileName));
        } catch (Exception e) {
            showError(e);
        }
    }

    public void createRemoteFolder(String fileName) {
        sendMessage(new FileCreateMessage(remoteRoot.resolve(fileName).toString()));
        ResultMessage resultMessage = (ResultMessage) receiveMessage();
        if (!resultMessage.isSuccessful()) {
            showError(resultMessage.getError());
        }
    }

    public void removeLocalFiles(String fileName) {
        Path filePath = localRoot.resolve(fileName);
        if (!Files.isDirectory(filePath)) {
            removeLocalFile(filePath);
            return;
        }
        logger.info("Removing local folder");
        try {
            Files.walk(filePath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(this::removeLocalFile);
        } catch (Exception e) {
            showError(e);
        }
    }

    public void removeLocalFile(Path path) {
        try {
            Files.delete(path);
            logger.info("Local file removed - " + path);
        } catch (Exception e) {
            showError(e);
        }
    }

    public void removeRemoteFile(String fileName) {
        logger.info("Sending remove message");
        sendMessage(new FileRemoveMessage(remoteRoot.resolve(fileName).toString()));
    }

    public void sendMessage(Message message) {
        try {
            network.sendMessage(message);
        } catch (Exception e) {
            showError(e);
        }
    }

    public Object receiveMessage() {
        try {
            return network.receiveMessage();
        } catch (Exception e) {
            showError(e);
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
        } catch (Exception e) {
            showError(e);
        }
        return new ArrayList<>();
    }

    public FileInfoMessage getLocalItem(Path path) {
        String fileName = localRoot.relativize(path).toString();
        try {
            long fileSize = Files.isDirectory(path) ? -1 : Files.size(path);
            return new FileInfoMessage(fileName, fileSize);
        } catch (Exception e) {
            return new FileInfoMessage(fileName, 0);
        }
    }

    @SuppressWarnings("unchecked")
    public List<FileInfoMessage> getRemoteItems() {
        logger.info("Getting remote items");
        System.out.println(remoteRoot);
        sendMessage(new FileListMessage(remoteRoot.toString()));
        Object object = receiveMessage();
        return (List<FileInfoMessage>) object;
    }

    private Network getNetwork() {
        try {
            Pair<String, String> pair = getServerInfo();
            System.out.println(pair.getKey() + " " + pair.getValue());
            String[] serverInfo = pair.getKey().split(":");
            Network network = new Network(serverInfo[0], Integer.parseInt(serverInfo[1]));
            Thread.sleep(1000);
            network.sendMessage(new AuthMessage(pair.getValue()));
            ResultMessage message = (ResultMessage) network.receiveMessage();
            if (!message.isSuccessful()) {
                showError(message.getError());
            } else {
                return network;
            }
        } catch (Exception ignored) {
        }
        showError(new Throwable("Failed to connect :("));
        System.exit(1);
        return null;
    }

    private Pair<String, String> getServerInfo() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Input Dialog");

        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField serverInfo = new TextField();
        TextField userName = new TextField();

        grid.add(new Label("Address:"), 0, 0);
        grid.add(serverInfo, 1, 0);
        grid.add(new Label("UserName:"), 0, 1);
        grid.add(userName, 1, 1);

        Node acceptButton = dialog.getDialogPane().lookupButton(loginButtonType);
        acceptButton.setDisable(true);

        serverInfo.textProperty().addListener((observable, oldValue, newValue) -> {
            acceptButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(serverInfo::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(serverInfo.getText(), userName.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();
        return result.orElse(null);
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
