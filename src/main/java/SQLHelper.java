import java.sql.*;

import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import Exceptions.ExceptionLogger;
import timeToLive.Forever;
import timeToLive.Limited;
import timeToLive.TimeToLive;
public class SQLHelper {

    private static Connection connection;

    public static void connectSqlite(String sqlPath) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            ExceptionLogger.log(e);
            System.exit(1);
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + sqlPath);
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS mainTable (" +
                        "id TEXT NOT NULL UNIQUE, " +
                        "ip TEXT NOT NULL, " +
                        "userAgent TEXT NOT NULL, " +
                        "isVisible	TEXT NOT NULL, " +
                        "downloadTime INTEGER NOT NULL, " +
                        "expirationTime INTEGER NOT NULL, " +
                        "jsonData	TEXT, " +
                        "PRIMARY KEY(id));")) {
            statement.execute();
        }

    }

    public static ConcurrentHashMap<String, GraphItem> readGraphsMap() throws SQLException, IOException, ClassNotFoundException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM mainTable;")) {
            ResultSet resultSet = statement.executeQuery();
            ConcurrentHashMap<String, GraphItem> map = new ConcurrentHashMap<>();
            while (resultSet.next()) {
                TimeToLive timeToLive;
                Date downloadTime = new Date(resultSet.getLong("downloadTime"));
                Date expirationTime = new Date(resultSet.getLong("expirationTime"));
                if (expirationTime.getTime() == 0) timeToLive = new Forever(downloadTime);
                else timeToLive = new Limited(downloadTime, expirationTime);

                String[] userAgentArray = resultSet.getString("userAgent").split("//-/---/-//");
                List<String> list = Arrays.asList(userAgentArray);

                GraphItem graphItem = new GraphItem(
                        resultSet.getString("isVisible").equals("true"),
                        timeToLive,
                        resultSet.getString("jsonData"),
                        resultSet.getString("ip"),
                        list,
                        resultSet.getString("id"));

                map.put(graphItem.getId(), graphItem);
            }
            return map;
        }
    }

    public synchronized static void addGraph(GraphItem graphItem) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO mainTable (id, ip, userAgent, isVisible, downloadTime, expirationTime, jsonData) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);")) {

            statement.setString(1, graphItem.getId());
            statement.setString(2, graphItem.getIp());
            StringJoiner joiner = new StringJoiner("//-/---/-//");
            for (String temp : graphItem.getUserAgent()) {
                joiner.add(temp);
            }
            statement.setString(3, joiner.toString());
            statement.setString(4, String.valueOf(graphItem.isVisible()));
            statement.setLong(5, graphItem.getTimeToLive().getDownloadTime().getTime());
            statement.setLong(6, graphItem.getTimeToLive().getExpirationTime().getTime());
            statement.setString(7, graphItem.getGraphBody());

            statement.execute();
        }
    }

    public static void deleteGraph(String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM mainTable WHERE id=(?)")) {
            statement.setString(1, id);
            statement.execute();
        }
    }
}
