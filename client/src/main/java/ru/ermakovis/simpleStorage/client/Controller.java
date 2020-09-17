package ru.ermakovis.simpleStorage.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.FileInfoMessage;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class Controller {
    private final Deque<Path> localHistory = new ArrayDeque<>();
    private final Deque<Path> localHistoryRedo = new ArrayDeque<>();
    private final Deque<Path> remoteHistory = new ArrayDeque<>();
    private final Deque<Path> remoteHistoryRedo = new ArrayDeque<>();
    private final Logger logger = LoggerFactory.getLogger(Controller.class);
    private final ObservableList<Pane> localItemsList = FXCollections.observableArrayList();
    private final ObservableList<Pane> remoteItemsList = FXCollections.observableArrayList();
    private Client client;

    @FXML
    private ListView<Pane> remoteItems;

    @FXML
    private ListView<Pane> localItems;

    @FXML
    void localCreateButtonAction(ActionEvent event) {

    }

    @FXML
    void localDeleteButtonAction(ActionEvent event) {

    }

    @FXML
    void localNextFolderButtonAction() {
        logger.info("LocalNextFolderButton pressed");
        if (localHistoryRedo.size() == 0) {
            return;
        }
        client.setLocalRoot(localHistoryRedo.pop());
        refreshLocalList();
    }

    @FXML
    void remoteNextFolderButtonAction() {
        logger.info("RemoteNextFolderButton pressed");
        if (remoteHistoryRedo.size() == 0) {
            return;
        }
        client.setRemoteRoot(remoteHistoryRedo.pop());
        refreshRemoteList();
    }

    @FXML
    void localPrevFolderButtonAction(ActionEvent event) {
        logger.info("LocalPrevFolder button pressed");
        if (localHistory.size() == 0) {
            return;
        }
        Path path = localHistory.pop();
        localHistoryRedo.push(client.getLocalRoot());
        client.setLocalRoot(path);
        refreshLocalList();
    }

    @FXML
    void remotePrevFolderButtonAction(ActionEvent event) {
        logger.info("RemotePrevFolderButton pressed");
        if (remoteHistory.size() == 0) {
            return;
        }
        Path path = remoteHistory.pop();
        remoteHistoryRedo.push(client.getRemoteRoot());
        client.setRemoteRoot(path);
        refreshRemoteList();
    }

    @FXML
    void localUpFolderButtonAction(ActionEvent event) {
        logger.info("LocalUpButton pressed");
        Path localPath = client.getLocalRoot();
        Path parentPath = localPath.getParent();
        if (parentPath == null) {
            return;
        }
        setLocalRoot(parentPath);
    }

    @FXML
    void remoteUpFolderButtonAction(ActionEvent ignored) {
        logger.info("remoteUpFolderButton pressed");
        Path remoteRoot = client.getRemoteRoot();
        Path parentPath = remoteRoot.getParent();
        if (parentPath == null) {
            parentPath = Path.of("");
        }
        setRemoteRoot(parentPath);
    }

    @FXML
    void localRefreshButtonAction(ActionEvent event) {
        logger.info("LocalRefreshButton pressed");
        refreshLocalList();
    }

    @FXML
    void remoteRefreshButtonAction(ActionEvent event) {
        logger.info("RemoteRefreshButton pressed");
        refreshRemoteList();
    }

    @FXML
    void remoteCreateButtonAction(ActionEvent event) {

    }

    @FXML
    void remoteDeleteButtonAction(ActionEvent event) {

    }

    @FXML
    void uploadButtonAction(ActionEvent ignored) {
        DisplayItem item = (DisplayItem) localItems.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        client.sendFiles(item.getFileName());
        refreshRemoteList();
    }

    @FXML
    void downloadButtonAction(ActionEvent ignored) {
        DisplayItem item = (DisplayItem) remoteItems.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        client.receiveFiles(item.getFileName());
        refreshLocalList();
    }

    public void setRemoteRoot(Path path) {
        logger.info("Setting remote root to " + path);
        if (remoteHistory.size() > 10) {
            remoteHistory.removeLast();
        }
        remoteHistory.addFirst(client.getRemoteRoot());
        client.setRemoteRoot(path);
        refreshRemoteList();
    }

    public void setLocalRoot(Path path) {
        logger.info("Setting local root to " + path);
        if (localHistory.size() > 10) {
            localHistory.removeLast();
        }
        localHistory.addFirst(client.getLocalRoot());
        client.setLocalRoot(path);
        refreshLocalList();
    }

    public void refreshLocalList() {
        refreshList(localItemsList, client.getLocalItems());
    }

    public void refreshRemoteList() {
        refreshList(remoteItemsList, client.getRemoteItems());
    }

    public void refreshList(ObservableList<Pane> list, List<FileInfoMessage> items) {
        list.clear();
        for (FileInfoMessage fileInfoMessage : items) {
            list.add(new DisplayItem(fileInfoMessage));
        }
    }

    public void initController(Client client) {
        this.client = client;
        localItems.setItems(localItemsList);
        remoteItems.setItems(remoteItemsList);
        localItems.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                DisplayItem item = (DisplayItem) localItems.getSelectionModel().getSelectedItem();
                setLocalRoot(client.getLocalRoot().resolve(item.getFileName()));
                localHistoryRedo.clear();
            }
        });
        remoteItems.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                DisplayItem item = (DisplayItem) remoteItems.getSelectionModel().getSelectedItem();
                setRemoteRoot(client.getRemoteRoot().resolve(item.getFileName()));
                remoteHistoryRedo.clear();
            }
        });

        refreshLocalList();
        refreshRemoteList();
    }
}
