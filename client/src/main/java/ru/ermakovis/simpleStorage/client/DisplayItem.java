package ru.ermakovis.simpleStorage.client;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import ru.ermakovis.simpleStorage.common.FileInfoMessage;

public class DisplayItem extends HBox {
    private final String fileName;

    public DisplayItem(FileInfoMessage message) {
        fileName = message.getName();
        Label nameLabel = new Label(fileName);

        Image image = new Image(message.getSize() == -1 ?
                getClass().getResource("/images/folder.png").toString() :
                getClass().getResource("/images/file.png").toString());

        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitWidth(15);
        imageView.setFitHeight(15);
        this.getChildren().addAll(imageView, nameLabel);
    }

    public String getFileName() {
        return fileName;
    }
}
