package ru.geekbrains.java3.client;


import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class Controller implements Initializable {



    private String nickname;
    private boolean isAuthSuccess;

    @FXML
    private HBox clientPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private TextField textField;

    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;
    @FXML
    private HBox authPanel;


    public void setAuth(boolean isAuthSuccess) {
        this.isAuthSuccess = isAuthSuccess;
        authPanel.setVisible(!isAuthSuccess);
        authPanel.setManaged(!isAuthSuccess);
        msgPanel.setVisible(isAuthSuccess);
        msgPanel.setManaged(isAuthSuccess);
        clientPanel.setVisible(isAuthSuccess);
        clientPanel.setManaged(isAuthSuccess);
        if (!isAuthSuccess) {
            nickname = "";
        }
    }

    public void sendAuth() {
        Network.sendAuth(loginField.getText(), passwordField.getText());
        loginField.clear();
        passwordField.clear();

    }
    public void sendMsg() {
        if (Network.sendMsg(textField.getText())) {
            textField.clear();
            textField.requestFocus();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuth(false);

        clientPanel.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2) {
            final String msg = textField.getText();
            String nickname = clientList.getSelectionModel().getSelectedItem();
            textField.setText("/w " + nickname + " " + msg);
            textField.requestFocus();
            textField.selectEnd();
        }
    });
    linkCallbacks();
}

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
            alert.showAndWait();
        });
    }
    public void linkCallbacks() {
        Network.setCallOnException(args -> showAlert(args[0].toString()));

        Network.setCallOnCloseConnection(args -> setAuth(false));

        Network.setCallOnAuthenticated(args -> {
            setAuth(true);
            nickname = args[0].toString();
        });

        Network.setCallOnMsgReceived(args -> {
            String msg = args[0].toString();
            if (msg.startsWith("/")) {
                if (msg.startsWith("/clients ")) {
                    String[] clients = msg.substring(9).split("\\s");
                    Platform.runLater(() -> {
                        clientList.getItems().clear();
                        for (String client : clients) {
                            clientList.getItems().add(client);
                        }
                    });
                }
                if (msg.startsWith("/changenick ")) {
                    nickname = msg.split("\\s")[1];
                }
            } else {
                textArea.appendText(msg + "\n");
            }
        });
    }
}
