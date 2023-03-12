package ru.geekbrains.java3.server;

public interface AuthService {
    String getNickname(String login, String pass);

     boolean changeNickname(String currentNickname, String newNickname);
}
