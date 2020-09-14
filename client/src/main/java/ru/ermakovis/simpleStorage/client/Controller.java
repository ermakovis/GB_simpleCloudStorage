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
    private ListView<String> localItems;

    @FXML
    private ListView<String> remoteItems;

    @FXML
    private Button uploadButton;

    @FXML
    private Button downloadButton;

    @FXML
    void downloadButtonAction(ActionEvent event) {
        System.out.println("DOWNLOAD!");
    }

    @FXML
    void uploadButtonAction(ActionEvent event) {
        System.out.println("UPLOAD!");
    }

    public void initController(Client client) {
        this.client = client;
        localItems.setItems(localItemsList);
        remoteItems.setItems(remoteItemsList);
        localItemsList.addAll(client.getLocalItems());
        remoteItemsList.addAll(client.getRemoteItems());
    }
}
