package ru.geekbrains.java3.server;

public class DataBaseAuth implements AuthService {


    @Override
    public boolean changeNickname(String currentNickname, String newNickname) {
        return DataBase.changeUserNickname(currentNickname, newNickname);
    }

    @Override
    public String getNickname(String login, String pass) {
        return DataBase.getUserNickname(login, pass);
    }
}
