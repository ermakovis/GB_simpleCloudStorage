package ru.ermakovis.simpleStorage.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.FileInfoMessage;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

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
    void localCreateButtonAction() {
        logger.info("LocalCreateButton pressed");
        client.createLocalFolder(getUserInput());
        refreshLocalList();
    }

    @FXML
    void remoteCreateButtonAction() {
        logger.info("RemoteCreateButton pressed");
        client.createRemoteFolder(getUserInput());
        refreshRemoteList();
    }

    public String getUserInput() {
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Folder creation dialog");
        textInputDialog.getDialogPane().setContentText("FolderName: ");
        Optional<String> result = textInputDialog.showAndWait();
        return result.orElse(null);
    }

    @FXML
    void localDeleteButtonAction() {
        logger.info("LocalDeleteButton pressed");
        DisplayItem item = getSelectedItem(localItems);
        if (item == null) {
            return;
        }
        client.removeLocalFiles(item.getFileName());
        refreshLocalList();
    }

    @FXML
    void remoteDeleteButtonAction() {
        logger.info("RemoteDeleteButton pressed");
        DisplayItem item = getSelectedItem(remoteItems);
        if (item == null) {
            return;
        }
        client.removeRemoteFile(item.getFileName());
        refreshRemoteList();
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
    void localPrevFolderButtonAction() {
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
    void remotePrevFolderButtonAction() {
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
    void localUpFolderButtonAction() {
        logger.info("LocalUpButton pressed");
        Path localPath = client.getLocalRoot();
        Path parentPath = localPath.getParent();
        if (parentPath == null) {
            return;
        }
        setLocalRoot(parentPath);
    }

    @FXML
    void remoteUpFolderButtonAction() {
        logger.info("remoteUpFolderButton pressed");
        Path remoteRoot = client.getRemoteRoot();
        Path parentPath = remoteRoot.getParent();
        if (parentPath == null) {
            parentPath = Path.of("");
        }
        setRemoteRoot(parentPath);
    }

    @FXML
    void localRefreshButtonAction() {
        logger.info("LocalRefreshButton pressed");
        refreshLocalList();
    }

    @FXML
    void remoteRefreshButtonAction() {
        logger.info("RemoteRefreshButton pressed");
        refreshRemoteList();
    }

    @FXML
    void uploadButtonAction() {
        DisplayItem item = getSelectedItem(localItems);
        if (item == null) {
            return;
        }
        client.sendFiles(item.getFileName());
        refreshRemoteList();
    }

    @FXML
    void downloadButtonAction() {
        DisplayItem item = getSelectedItem(remoteItems);
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

    public DisplayItem getSelectedItem(ListView<Pane> listView) {
        return (DisplayItem) listView.getSelectionModel().getSelectedItem();
    }

    public void initController(Client client) {
        this.client = client;
        localItems.setItems(localItemsList);
        remoteItems.setItems(remoteItemsList);
        localItems.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                setLocalRoot(client.getLocalRoot().resolve(getSelectedItem(localItems).getFileName()));
                localHistoryRedo.clear();
            }
        });
        remoteItems.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                setRemoteRoot(client.getRemoteRoot().resolve(getSelectedItem(remoteItems).getFileName()));
                remoteHistoryRedo.clear();
            }
        });

        refreshLocalList();
        refreshRemoteList();
    }
}
