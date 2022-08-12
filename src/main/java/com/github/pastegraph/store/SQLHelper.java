package com.github.pastegraph.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class SQLHelper {

    private static Connection connection;
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLHelper.class);

    public static void connectSqlite(String sqlPath) {
        try {
            LOGGER.debug("Starting DB connection");
            Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
            connection = DriverManager.getConnection("jdbc:sqlite:" + sqlPath);
        } catch (Exception e) {
            LOGGER.error("Can't make DB connection ", e);
            System.exit(1);
        }

        LOGGER.debug("Initializing DB table");
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS graphData (" +
                        "id TEXT NOT NULL UNIQUE, " +
                        "ip TEXT NOT NULL, " +
                        "userAgent TEXT NOT NULL, " +
                        "isVisible	TEXT NOT NULL, " +
                        "uploadTime TEXT NOT NULL, " +
                        "expirationTime INTEGER NOT NULL, " +
                        "graphData	TEXT, " +
                        "PRIMARY KEY(id));")) {
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("Can't make DB table ", e);
            System.exit(1);
        }
        LOGGER.info("DB connected successfully");
    }

    public static ConcurrentHashMap<String, GraphItem> readGraphsMap() throws SQLException {
        LOGGER.debug("Starting reading Graph Items from DB");
        ConcurrentHashMap<String, GraphItem> map = new ConcurrentHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM graphData;")) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                GraphItem graphItem = new GraphItem(
                        resultSet.getString("isVisible").equals("true"),
                        new Date(resultSet.getLong("expirationTime")),
                        new Date(resultSet.getLong("uploadTime")),
                        resultSet.getString("graphData"),
                        resultSet.getString("ip"),
                        resultSet.getString("userAgent"),
                        resultSet.getString("id"));
                map.put(graphItem.getId(), graphItem);
            }
            LOGGER.debug("Items were red successfully");
            return map;
        }
    }

    public synchronized static void addGraph(GraphItem graphItem) throws SQLException, IOException {
        LOGGER.debug("Starting adding new Graph to DB");
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO graphData (id, ip, userAgent, isVisible, expirationTime, uploadTime, graphData) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);")) {

            statement.setString(1, graphItem.getId());
            statement.setString(2, graphItem.getIp());
            statement.setString(3, graphItem.getUserAgent());
            statement.setString(4, String.valueOf(graphItem.isVisible()));
            statement.setLong(5, graphItem.getExpirationTime().getTime());
            statement.setLong(6, graphItem.getUploadTime().getTime());
            statement.setString(7, graphItem.getGraphBody());

            statement.execute();
            LOGGER.info("A graph {} was added to DB", graphItem.getId());
        }
    }

    public static void deleteGraph(String id) throws SQLException {
        LOGGER.debug("Starting deleting Graph {} from DB", id);
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM graphData WHERE id=(?)")) {
            statement.setString(1, id);
            statement.execute();
        }
        LOGGER.info("Graph {} was deleted from DB", id);
    }
}
