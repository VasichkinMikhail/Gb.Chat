package ru.geekbrains.java3.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {

    private final AuthService authService;

    private final List<ClientHandler> clients;

    private ExecutorService clientsExecutorService;

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());


    public ChatServer() {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        clients = new ArrayList<>();

        if (DataBase.run()) {
            RuntimeException e = new RuntimeException("Невозможно подключится к базе данных");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;

        }
        authService = new DataBaseAuth();
        clientsExecutorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(8180)) {
            logger.log(Level.INFO, "Сервер подключается к порту  " + serverSocket.getLocalPort() + "...");
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket,this);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            DataBase.disconnect();
            logger.log(Level.INFO, "Сервер закрыт!");
        }
    }
    public ExecutorService getClientsExecutorService() {
        return clientsExecutorService;
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
        logger.log(Level.FINEST,"Пользователь " + sendNick + " отправил сообщение!");
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(sendNick)) {
                client.sendMsg("You: " + msg);
            } else {
                client.sendMsg(sendNick + ": " + msg);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        logger.log(Level.FINE, "Пользователь  " + clientHandler.getNickname() + " подключился");
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        logger.log(Level.FINE, "Пользователь " + clientHandler.getNickname() + " отключился");
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
            logger.log(Level.FINEST,"Пользователь " + from.getNickname() + " отправил заметку");
            return;
        }
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickTo)) {
                client.sendMsg(from.getNickname() + " whispered " + ": " + msg);
                from.sendMsg("Ваш приватное сообщение для : " + nickTo + ": " + msg);
                logger.log(Level.FINEST,"Пользователь " + from.getNickname() + " отправил приватное сообщение");
                return;
            }
        }
        from.sendMsg("Участника с ником " + nickTo + " нет в чат-комнате");
        logger.log(Level.FINEST,
                "От " + from.getNickname() + " попытка отправить сообщение отсутствующему в чате пользователю");
    }
}
