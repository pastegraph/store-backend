import java.sql.*;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import Exceptions.ExceptionLogger;
import timeToLive.*;
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
                "CREATE TABLE IF NOT EXISTS graphData (" +
                        "id TEXT NOT NULL UNIQUE, " +
                        "ip TEXT NOT NULL, " +
                        "userAgent TEXT NOT NULL, " +
                        "isVisible	TEXT NOT NULL, " +
                        "uploadTime INTEGER NOT NULL, " +
                        "expirationTime INTEGER NOT NULL, " +
                        "graphData	TEXT, " +
                        "PRIMARY KEY(id));")) {
            statement.execute();
        }

    }

    public static ConcurrentHashMap<String, GraphItem> readGraphsMap() throws SQLException, IOException, ClassNotFoundException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM graphData;")) {
            ResultSet resultSet = statement.executeQuery();
            ConcurrentHashMap<String, GraphItem> map = new ConcurrentHashMap<>();
            while (resultSet.next()) {
                GraphTime graphTime;
                Date uploadTime = new Date(resultSet.getLong("uploadTime"));
                Date expirationTime = new Date(resultSet.getLong("expirationTime"));
                if (expirationTime.getTime() == 0) graphTime = new Forever(uploadTime);
                else graphTime = new Limited(uploadTime, expirationTime);

                GraphItem graphItem = new GraphItem(
                        resultSet.getString("isVisible").equals("true"),
                        graphTime,
                        resultSet.getString("graphData"),
                        resultSet.getString("ip"),
                        resultSet.getString("userAgent"),
                        resultSet.getString("id"));

                map.put(graphItem.getId(), graphItem);
            }
            return map;
        }
    }

    public synchronized static void addGraph(GraphItem graphItem) throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO graphData (id, ip, userAgent, isVisible, uploadTime, expirationTime, graphData) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);")) {

            statement.setString(1, graphItem.getId());
            statement.setString(2, graphItem.getIp());
            statement.setString(3, graphItem.getUserAgent());
            statement.setString(4, String.valueOf(graphItem.isVisible()));
            statement.setLong(5, graphItem.getTimeToLive().getUploadTime().getTime());
            statement.setLong(6, graphItem.getTimeToLive().getExpirationTime().getTime());
            statement.setString(7, graphItem.getGraphBody());

            statement.execute();
        }
    }

    public static void deleteGraph(String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM graphData WHERE id=(?)")) {
            statement.setString(1, id);
            statement.execute();
        }
    }
}
