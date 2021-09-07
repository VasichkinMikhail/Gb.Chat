package ru.geekbrains.june.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    public String username;
    private DataInputStream in;
    private DataOutputStream out;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) {//Создали потоки,соединение и сервер. Открыли в потоке Логику
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> logic()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logic() {//Логика.Обратились и ожидаем ответа от методов. В случае отключения клиента раскидываем всем инфу об отключении
        try {
            while (!consumeAuthorizeMessage(in.readUTF())) ;
            while (consumeRegularMessage(in.readUTF()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Клиент " + username + " отключился");
            server.unsubscribe(this);
            closeConnection();
        }

    }

    private boolean consumeRegularMessage(String inputMessage) {//Установка служебных команд как выход из авторизации
//и отправка сообщений в личку клиенту минуя общий чат
        if (inputMessage.startsWith("/")) {
            if (inputMessage.equals("/exit")) {
                sendMessage("/exit");
                return false;
            }
            if (inputMessage.startsWith("/w ")) {
                String[] tokens = inputMessage.split("\\s+", 3);
                server.sendPersonalMessage(this, tokens[1], tokens[2]);
            }
            return true;
        }
        server.broadcastMessage(username + ": " + inputMessage);
        return false;
    }
    private boolean consumeAuthorizeMessage(String message){//Обрабатываем все возможности не правильного ввода имени при авторизации.
            if (message.startsWith("/auth ")) {//такие как отсутствие ввода, имя из нескольких слов, имя которое уже присвоено,
                String[] tokens = message.split("\\s+");//и отбивка в случае правильного выполнения.

                if (tokens.length == 1) {
                    sendMessage("Server: Вы не указали имя пользователя");
                    return false;
                }
                if (tokens.length > 2) {
                    sendMessage("Server: Имя не может состоять из нескольких слов");
                    return false;
                }
                String selectedUsername = tokens[1];
                if (server.isUserNameUsed(selectedUsername)) {
                    sendMessage("Server: Данное имя уже используется");
                    return false;
                }
                username = selectedUsername;
                sendMessage("/authok");
                server.subscribe(this);
                return true;
            } else {
                sendMessage("Server: Вам необходимо авторизоваться!");
                return false;
            }
        }

    public void sendMessage(String message) {//Отправка сообщений
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void closeConnection() { //Закрытие потоков и соединения
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
}