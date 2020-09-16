package ru.ermakovis.simpleStorage.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller {
    private final ObservableList<String> localItemsList = FXCollections.observableArrayList();
    private final ObservableList<String> remoteItemsList = FXCollections.observableArrayList();
    private Client client;

    @FXML
    private ListView<String> remoteItems;

    @FXML
    private ListView<String> localItems;

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
    void downloadButtonAction(ActionEvent event) {

    }

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
    void localRefreshButtonAction(ActionEvent event) {

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
    void remoteRefreshButtonAction(ActionEvent event) {

    }

    @FXML
    void uploadButtonAction(ActionEvent event) {

    }

    public void initController(Client client) {
        this.client = client;
        localItems.setItems(localItemsList);
        remoteItems.setItems(remoteItemsList);
        localItemsList.addAll(client.getLocalItems());
        remoteItemsList.addAll(client.getRemoteItems());
    }
}
