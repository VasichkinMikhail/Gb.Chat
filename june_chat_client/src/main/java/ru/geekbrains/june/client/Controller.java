package ru.geekbrains.june.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {
     @FXML
     TextArea chatArea;

     @FXML
     TextField messageField, userNameField, nameTitle;

     @FXML
     HBox authPanel, msgPanel;

     @FXML
     ListView<String> clientsListView;






     private Socket socket;
     private DataInputStream in;
     private DataOutputStream out;


     public void setAuthorized(boolean authorized){//При выполнение авторизации мы делаем не видимым и не выделяем место под поле ввода имени.
          msgPanel.setVisible(authorized);//И делаем видимыми область ввода сообщений и список клиентов.
          msgPanel.setManaged(authorized);
          authPanel.setVisible(!authorized);
          authPanel.setManaged(!authorized);
          clientsListView.setVisible(authorized);
          clientsListView.setManaged(authorized);
          nameTitle.setVisible(authorized);
          nameTitle.setManaged(authorized);

     }


     public void sendMessage() {
          try {
               out.writeUTF(messageField.getText());
               messageField.clear();
               messageField.requestFocus();
          } catch (IOException e) {
               showError("Невозможно отправить запрос авторизации на сервер");
          }
     }
     public void sendCloseRequest() {//выход
          try {
               if (out!=null) {
                    out.writeUTF("/exit");
               }
          } catch (IOException e) {
               e.printStackTrace();
          }
     }



     public void tryToAuth() {//Авторизация на сервере
          connect();
          try {
               out.writeUTF("/auth " + userNameField.getText());
               nameTitle.appendText(userNameField.getText() + " online");
               userNameField.clear();

          }catch (IOException e){
               showError("Невозможно отправить запрос авторизации на сервер");
          }


     }
     public void connect(){//Соединения с хостом и портом и открытие Логики в потоке.
          if (socket != null && !socket.isClosed()){
               return;
          }
          try {
               socket = new Socket("localhost", 8190);
               in = new DataInputStream(socket.getInputStream());
               out = new DataOutputStream(socket.getOutputStream());
               new Thread(() -> logic()).start();
          } catch (IOException e) {
               showError("Невозможно подключиться к серверу!");
          }
     }
     private void logic(){//Логика: Использование служебных команд, и формирование списка клиентов.
          try {
               while (true) {
                    String inputMessage = in.readUTF();
                    if (inputMessage.equals("/exit")) {
                         closeConnection();
                    }
                    if (inputMessage.equals("/authok")) {
                         setAuthorized(true);
                         break;
                    }
                    chatArea.appendText(inputMessage + "\n");
               }
               while (true) {
                    String inputMessage = in.readUTF();
                    if (inputMessage.startsWith("/")) {
                         if (inputMessage.equals("/exit")) {
                              break;
                         }
                         if (inputMessage.startsWith("/clients_list ")) {
                              Platform.runLater(() -> {
                                   String[] tokens = inputMessage.split("\\s+");
                                   clientsListView.getItems().clear();
                                   for (int i = 0; i < tokens.length; i++) {
                                        clientsListView.getItems().add(tokens[i]);

                                   }
                              });
                         }
                              continue;
                         }
                         chatArea.appendText(inputMessage + "\n");
                    }
               }catch(IOException e){
                    e.printStackTrace();
               }finally{
                    closeConnection();
               }

          }

     public void showError(String message){
          new Alert(Alert.AlertType.ERROR,message, ButtonType.OK).showAndWait();

     }
     private void closeConnection() {
          setAuthorized(false);
          try {
               if (in != null) {
                    in.close();
               }
          } catch (IOException e) {
               e.printStackTrace();
          }
          try {
               if (out != null) {
                    out.close();
               }
          } catch (IOException e) {
               e.printStackTrace();
          }
          try {
               if (socket != null) {
                    socket.close();
               }
          } catch (IOException e) {
               e.printStackTrace();
          }
     }
     public void clientListDoubleClick(MouseEvent mouseEvent){//Возможность двойным кликом мышки по имени в списке клиентов начать личное общение обходя общий чат.
          if (mouseEvent.getClickCount()==2){
               String selectedUser = clientsListView.getSelectionModel().getSelectedItem();
               messageField.setText("/w " + selectedUser + " ");
               messageField.requestFocus();
               messageField.selectEnd();
          }
     }

}
