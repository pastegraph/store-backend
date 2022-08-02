package com.github.pastegraph.store;

import com.github.pastegraph.store.Exceptions.ExceptionLogger;
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

    public static void main(String...args) {
        String sqlPath = args.length == 1 ? args[0] : System.getProperty("user.home") + File.separator + "pastegraph.s3db";
        SQLHelper.connectSqlite(sqlPath);
        try {
            graphsMap = SQLHelper.readGraphsMap();
        } catch (SQLException e) {
            ExceptionLogger.log(e);
            System.exit(1);
        }

        startServer();
        startTimeDaemon();
    }

    private static void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new Handler(graphsMap));
            server.start();
        } catch (IOException e) {
            ExceptionLogger.log(e);
        }
    }

    private static void startTimeDaemon() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    graphsMap = SQLHelper.readGraphsMap();
                } catch (SQLException e) {
                    ExceptionLogger.log(e);
                }
                Iterator<String> mapIterator = graphsMap.keys().asIterator();
                while (mapIterator.hasNext()) {
                    String current = mapIterator.next();
                    if (graphsMap.get(current).getExpirationTime().before(new Date())) {
                        graphsMap.remove(current);
                        try {
                            SQLHelper.deleteGraph(current);
                        } catch (SQLException e) {
                            ExceptionLogger.log(e);
                        }
                    }
                }
            }
        };
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(task, 0, 60000);
    }
}
