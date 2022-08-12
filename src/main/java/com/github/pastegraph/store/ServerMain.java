package com.github.pastegraph.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {

    private static final int port = 8080;
    private static ConcurrentHashMap<String, GraphItem> graphsMap;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String... args) {
        LOGGER.info("Application started");

        String sqlPath = args.length == 1 ? args[0] : System.getProperty("user.home") + File.separator + "pastegraph.s3db";
        LOGGER.debug("sqlPath = {}", sqlPath);

        try {
            SQLHelper.connectSqlite(sqlPath);

            graphsMap = SQLHelper.readGraphsMap();
            LOGGER.debug("Graphs map was read");
        } catch (SQLException e) {
            LOGGER.error("Caught an exception in main. SQL error {}", e.getMessage());
            System.exit(1);
        }

        LOGGER.info("Starting server");
        startServer();

        LOGGER.debug("Starting TimeDaemon");
        startTimeDaemon();
    }

    private static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new Handler(graphsMap));
            server.start();
            LOGGER.info("Server started successfully");
        } catch (IOException e) {
            LOGGER.error("Caught an IO exception. {}", e.getMessage());
        }
    }

    private static void startTimeDaemon() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                LOGGER.debug("Time Daemon starts");
                try {
                    graphsMap = SQLHelper.readGraphsMap();
                } catch (SQLException e) {
                    LOGGER.warn("Time Daemon failed to read DB");
                    return;
                }
                Iterator<String> mapIterator = graphsMap.keys().asIterator();
                while (mapIterator.hasNext()) {
                    String current = mapIterator.next();
                    if (graphsMap.get(current).getExpirationTime().getTime() != 0 &&
                            graphsMap.get(current).getExpirationTime().before(new Date())) {

                        graphsMap.remove(current);
                        try {
                            SQLHelper.deleteGraph(current);
                            LOGGER.debug("Time Daemon deleted a graph item {}", current);
                        } catch (SQLException e) {
                            LOGGER.warn("Time Daemon can't delete a graph {} from DB. {}", current, e.getMessage());
                        }
                    }
                }
                LOGGER.debug("Time Daemon finished it's work");
            }
        };
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(task, 0, 10000);
        LOGGER.debug("Time Daemon was scheduled");
    }
}
