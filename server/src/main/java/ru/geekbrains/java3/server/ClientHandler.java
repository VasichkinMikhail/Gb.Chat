package ru.geekbrains.java3.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler {
    private  Socket socket;
    private  ChatServer server;
    private  DataInputStream in;
    private  DataOutputStream out;
    private static final Logger logger = Logger.getLogger(DataBase.class.getName());

    public String getNickname() {
        return nickname;
    }

    private String nickname;


    public ClientHandler(Socket socket, ChatServer server) {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            server.getClientsExecutorService().execute(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            logger.log(Level.FINE, "Пользователь пытается авторизироватся!");
                            String[] tokens = msg.split("\\s");
                            String nickname = server.getAuthService().getNickname(tokens[1], tokens[2]);
                            if (nickname != null && !server.isNicknameBusy(nickname)) {
                                sendMsg("/auth: онлайн " + nickname);
                                this.nickname = nickname;
                                server.subscribe(this);
                                break;
                            }
                            logger.log(Level.FINE, "Пользователь не смог авторизироватся!");
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.equals("/end")) {
                                sendMsg("/end");
                                break;
                            }
                            if (msg.startsWith("/w ")) {
                                String[] tokens = msg.split("\\s", 3);
                                server.sendMsgToClient(this, tokens[1], tokens[2]);
                            }
                            if (msg.startsWith("/changenick ")) {
                                logger.log(Level.FINER,"Пользователь " + this.nickname + " пытается сменить свой nickname");
                                String newNickname = msg.split("\\s", 2)[1];
                                if (!newNickname.matches("([a-zA-Z]+[0-9]*)|([а-яА-Я]+[0-9]*)")) {
                                    sendMsg("/changenick:error Nickname должен содержать только буквы и цифры!");
                                    logger.log(Level.FINER,"Пользователь  " + this.nickname +
                                            " новый ник содержит не допустимые символы!");
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(this.nickname, newNickname)) {
                                    logger.log(Level.FINER,"Пользователь " + this.nickname +
                                            " изменил свой nickname свой  " + newNickname);
                                    this.nickname = newNickname;
                                    sendMsg("/changenick: изменён " + nickname);
                                    server.broadcastClientsList();
                                } else {
                                    sendMsg("Nickname уже используется");
                                    logger.log(Level.FINER,"Пользователь " + this.nickname +
                                            " nickname уже используется!");
                                }
                            }
                        } else {
                            server.broadcast(this.nickname, msg);
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    e.printStackTrace();

                } finally {
                    ClientHandler.this.disconnect();
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            e.printStackTrace();
        }
    }


    public void disconnect() {
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
