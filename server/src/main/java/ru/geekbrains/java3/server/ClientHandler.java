package ru.geekbrains.java3.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private  Socket socket;
    private  ChatServer server;
    private  DataInputStream in;
    private  DataOutputStream out;

    public String getNickname() {
        return nickname;
    }

    private String nickname;


    public ClientHandler(Socket socket, ChatServer server) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            String[] tokens = msg.split("\\s");
                            String nickname = server.getAuthService().getNickname(tokens[1], tokens[2]);
                            if (nickname != null && !server.isNicknameBusy(nickname)) {
                                sendMsg("/auth " + nickname);
                                this.nickname = nickname;
                                server.subscribe(this);
                                break;
                            }
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
                                String newNickname = msg.split("\\s", 2)[1];
                                if (newNickname.contains(" ")) {
                                    sendMsg("Nickname cannot contain spaces");
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(this.nickname, newNickname)) {
                                    this.nickname = newNickname;
                                    sendMsg("/changenick " + nickname);
                                    sendMsg("Nickname has been changed");
                                    server.broadcastClientsList();
                                } else {
                                    sendMsg("Nickname is already taken");
                                }
                            }
                        } else {
                            server.broadcast(nickname + ": " + msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ClientHandler.this.disconnect();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
