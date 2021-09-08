package ru.geekbrains.java3.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {

    private final AuthService authService;

    private final List<ClientHandler> clients;


    public ChatServer() {
        clients = new ArrayList<>();

        if (DataBase.run()) {
            throw new RuntimeException("Невозможно подключится!");
        }
        authService = new DataBaseAuth();

        try (ServerSocket serverSocket = new ServerSocket(8180)) {
            System.out.println("SERVER: запущен...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("SERVER: Клиент подключился...");
                new ClientHandler(socket,this);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DataBase.disconnect();
            System.out.println("SERVER: закрыт");
        }
    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder(15 * clients.size());
        sb.append("/clients ");
        for (ClientHandler client : clients) {
            sb.append(client.getNickname()).append(" ");
        }
        sb.setLength(sb.length() - 1);
        String out = sb.toString();
        for (ClientHandler client : clients) {
            client.sendMsg(out);
        }
    }

    public void broadcast(String sendNick, String msg) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(sendNick)) {
                client.sendMsg("You: " + msg);
            } else {
                client.sendMsg(sendNick + ": " + msg);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        System.out.println("SERVER: Клиент " + clientHandler.getNickname() + " login...");
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        System.out.println("SERVER: Клиент " + clientHandler.getNickname() + " logout...");
        clients.remove(clientHandler);
        broadcastClientsList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNicknameBusy(String nickname) {
        for (ClientHandler client : clients) {
            if(client.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    public void sendMsgToClient(ClientHandler from, String nickTo, String msg) {
        if (from.getNickname().equals(nickTo)) {
            from.sendMsg("Note: " + msg);
            return;
        }
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickTo)) {
                client.sendMsg(from.getNickname() + " whispered " + ": " + msg);
                from.sendMsg("Ваш приватное сообщение для : " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Участника с ником " + nickTo + " нет в чат-комнате");
    }
}
