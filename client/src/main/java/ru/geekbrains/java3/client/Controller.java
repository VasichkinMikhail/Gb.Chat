package ru.geekbrains.java3.client;


import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.io.*;

public class Controller implements Initializable {


    private String nickname;
    private boolean isAuthSuccess;
    private File messageHistory;
    private final String you = " (You)";
    private final int rec_messages = 100;

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
        if (!textField.getText().equals("")) {
            if (Network.sendMsg(textField.getText())) {
                textField.clear();
                textField.requestFocus();
            }
            textField.requestFocus();
        }
    }

    private File setMessageHistory() {
        messageHistory = new File("C:\\Users\\budar\\IdeaProjects\\june_chat_1\\client\\dataUser\\user.txt");
        return messageHistory;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuth(false);

        clientPanel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                final String msg = textField.getText();
                String nickname = clientList.getSelectionModel().getSelectedItem().replace(you, "");
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
        int lineNumber = 0;

        if (setMessageHistory().exists()) {
            try (FileReader fr = new FileReader(messageHistory)) {
                LineNumberReader lnr = new LineNumberReader(fr);
                while (lnr.readLine() != null) {
                    lineNumber++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (BufferedReader br = new BufferedReader(new FileReader(messageHistory))) {
                for (int i = 0; i < lineNumber; i++) {
                    String line = br.readLine();
                    if (i >= lineNumber - rec_messages) {
                        textArea.appendText(line + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                messageHistory.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Network.setCallOnMsgReceived(args -> {
            String msg = args[0].toString();
            if (msg.startsWith("/")) {
                if (msg.startsWith("/clients ")) {
                    String[] clients = msg.split("\\s", 2)[1].split("\\s");
                    Platform.runLater(() -> {
                        clientList.getItems().clear();
                        clientList.getItems().add(nickname + you);
                        for (String client : clients) {
                            if (!client.equals(nickname)) {
                                clientList.getItems().add(client);
                            }
                        }
                    });
                }
                if (msg.startsWith("/changenick:")) {
                    if (msg.startsWith("/changenick:ошибка ")) {
                        String errorText = msg.split("\\s", 2)[1];
                        textArea.appendText(errorText + "\n");
                        return;
                    }
                    if (msg.startsWith("/changenick: online ")) {
                        nickname = msg.split("\\s")[1];
                        textArea.appendText("Nickname изменён\n");
                        messageHistory.renameTo(setMessageHistory());
                    }
                }
            } else {
                textArea.appendText(msg + "\n");
                try (FileWriter fw = new FileWriter(messageHistory, true)) {
                    fw.append(msg + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
