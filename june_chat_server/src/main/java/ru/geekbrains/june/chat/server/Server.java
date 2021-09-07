package ru.geekbrains.june.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server{
    private List<ClientHandler> clients;



    public Server() {
        try {
            this.clients = new ArrayList<>();//Подключение  клиента к серверу
            ServerSocket serverSocket = new ServerSocket(8199);
            System.out.println("Сервер запущен. Ожидаем подключение клиентов...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this,socket);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public synchronized void subscribe(ClientHandler c) {//Информирование участников о входе в чат нового участника.
        broadcastMessage("В чат зашел пользователь: " + c.getUsername());
        clients.add(c);
        broadcastClientList();

    }
    public synchronized void unsubscribe(ClientHandler c){//Оповещение участников чата о выходе их чата одного из клиентов.
        clients.remove(c);
        broadcastMessage(c.getUsername() + " вышел из чата" );
        broadcastClientList();
    }

    public synchronized void broadcastMessage(String message) {//Рассылка сообщений всем клиентам
        for (ClientHandler c : clients) {
            c.sendMessage(message);
        }
    }
    public synchronized void broadcastClientList() { //Список клиентов
        StringBuilder builder = new StringBuilder(clients.size() * 10);
        builder.append("/clients_list ");
        for (ClientHandler c : clients) {
                builder.append(c.getUsername()).append(" ");
            }
            String clientsListStr = builder.toString();
            broadcastMessage(clientsListStr);
        }
    public synchronized boolean isUserNameUsed(String username){//Недопущение использование в чате разных людей с одним именем
        for (ClientHandler c : clients){
            if (c.getUsername().equalsIgnoreCase(username)){
                return true;
            }
        }
        return false;
    }
    public synchronized void sendPersonalMessage(ClientHandler sender, String receiverUsername, String message){
       if (sender.getUsername().equalsIgnoreCase(receiverUsername)){//Отправка личных сообщений в обход общего чата
           sender.sendMessage("Нельзя отправлять сообщения самому себе");
           return;
       }
        for (ClientHandler c : clients){
            if (c.getUsername().equalsIgnoreCase(receiverUsername)){
                c.sendMessage("Сообщение от " + sender.getUsername() + ": " + message);
                sender.sendMessage("Пользователь " + receiverUsername + ": " + message);
                return;
            }
        }
        sender.sendMessage("Пользователя " + receiverUsername + " нет в сети!");
    }
}
