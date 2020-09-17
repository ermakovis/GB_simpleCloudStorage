package ru.ermakovis.simpleStorage.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ermakovis.simpleStorage.common.FileInfoMessage;

import java.nio.file.Path;
import java.util.List;

public class Controller {
    private final Logger logger = LoggerFactory.getLogger(Controller.class);
    private final ObservableList<Pane> localItemsList = FXCollections.observableArrayList();
    private final ObservableList<Pane> remoteItemsList = FXCollections.observableArrayList();
    private Client client;

    @FXML
    private ListView<Pane> remoteItems;

    @FXML
    private ListView<Pane> localItems;

    @FXML
    private Button localRefreshButton;

    @FXML
    private Button localCreateButton;

    @FXML
    private Button localDeleteButton;

    @FXML
    private Button localUpFolderButton;

    @FXML
    private Button localPrevFolderButton;

    @FXML
    private Button localNextFolderButton;


    @FXML
    private Button uploadButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button remoteRefreshButton;

    @FXML
    private Button remoteCreateButton;

    @FXML
    private Button remoteDeleteButton;

    @FXML
    private Button remoteUpFolderButton;

    @FXML
    private Button remotePrevFolderButton;

    @FXML
    private Button remoteNextFolderButton;

    @FXML
    void localCreateButtonAction(ActionEvent event) {

    }

    @FXML
    void localDeleteButtonAction(ActionEvent event) {

    }

    @FXML
    void localNextFolderButtonAction(ActionEvent event) {

    }

    @FXML
    void localPrevFolderButtonAction(ActionEvent event) {

    }

    @FXML
    void localUpFolderButtonAction(ActionEvent event) {
        logger.info("LocalUpButton pressed");
        Path parentPath = client.getLocalRoot().getParent();
        if (parentPath == null) {
            return;
        }
        client.setLocalRoot(parentPath);
        refreshLocalList();
    }

    @FXML
    void remoteUpFolderButtonAction(ActionEvent ignored) {
        logger.info("remoteUpFolderButton pressed");
        Path parentPath = client.getRemoteRoot().getParent();
        if (parentPath == null) {
            client.setRemoteRoot(Path.of(""));
        }
        client.setRemoteRoot(parentPath);
        refreshRemoteList();
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
    void remoteNextFolderButtonAction(ActionEvent event) {

    }

    @FXML
    void remotePrevFolderButtonAction(ActionEvent event) {

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
                client.setLocalRoot(client.getLocalRoot().resolve(item.getFileName()));
                refreshLocalList();
            }
        });
        remoteItems.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getClickCount() == 2) {
                DisplayItem item = (DisplayItem) remoteItems.getSelectionModel().getSelectedItem();
                client.setRemoteRoot(client.getRemoteRoot().resolve(item.getFileName()));
                refreshRemoteList();
            }
        });

        refreshLocalList();
        refreshRemoteList();
    }
}
