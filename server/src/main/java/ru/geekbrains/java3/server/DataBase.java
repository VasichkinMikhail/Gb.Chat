package ru.geekbrains.java3.server;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataBase{
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement getUserNicknameStatement;
    private static PreparedStatement changeUserNicknameStatement;
    private static PreparedStatement createUserStatement;
    private static final Logger logger = Logger.getLogger(DataBase.class.getName());


    public static boolean run() {
        logger.setLevel(Level.ALL);
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:javadb.db");
            logger.log(Level.INFO, "База данных подключена");
            statement = connection.createStatement();
            createTable();
            insert("Bob","321","Boby88");
            prepareAllStatement();
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            return false;
        }
    }

    private static void createTable() throws SQLException {
        statement.executeUpdate("create table if not exists BaseUserChat(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "login  TEXT," +
                "pass TEXT,"+
                "nickname TEXT"+
                ")");

    }

    public static void insert(String login,String pass, String nickname) throws SQLException {
        try (final PreparedStatement ps = connection.prepareStatement
                ("insert into BaseUserChat (login,pass,nickname) values (?,?,?)")) {
            ps.setString(1, login);
            ps.setString(2, pass);
            ps.setString(3, nickname);
            ps.executeUpdate();
        } catch (Exception e) {
            throw e;
        }
    }
    public static void prepareAllStatement() throws SQLException {
        getUserNicknameStatement = connection.prepareStatement("SELECT nickname FROM BaseUserChat WHERE login = ? AND pass = ?;");
        changeUserNicknameStatement = connection.prepareStatement("UPDATE BaseUserChat SET nickname = ? WHERE nickname = ?;");

    }


    public static String getUserNickname(String login, String pass) {
        String nickname = null;
        try {
            getUserNicknameStatement.setString(1, login);
            getUserNicknameStatement.setString(2, pass);
            ResultSet rs = getUserNicknameStatement.executeQuery();
            if (rs.next()) {
                nickname = rs.getString(1);
            }
            rs.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return nickname;
    }

    public static void disconnect() {
        try {
            statement.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    public static boolean changeUserNickname(String currentNickname, String newNickname) {
        try {
            changeUserNicknameStatement.setString(1, newNickname);
            changeUserNicknameStatement.setString(2, currentNickname);
            changeUserNicknameStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


}
