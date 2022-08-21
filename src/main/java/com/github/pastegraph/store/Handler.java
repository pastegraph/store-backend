package com.github.pastegraph.store;

import com.github.pastegraph.store.Exceptions.CantCastJSONException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Handler implements HttpHandler {
    private final ConcurrentHashMap<String, String> CORRECT_REQUESTS = new ConcurrentHashMap<>();
    private final String REQUEST_DESCRIPTION;
    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);

    {
        CORRECT_REQUESTS.put("getGraph", " - get graph body by ID. Example: /getGraph/12345\n");
        CORRECT_REQUESTS.put("getAllGraphs", " - get all graphs IDs\n");

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : CORRECT_REQUESTS.entrySet())
            builder.append(entry.getKey()).append(entry.getValue());
        REQUEST_DESCRIPTION = builder.toString();
    }
    private ConcurrentHashMap<String, GraphItem> graphsMap;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Handler(ConcurrentHashMap<String, GraphItem> graphsMap) {
        this.graphsMap = graphsMap;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            LOGGER.info("Handler got a request. ip: {}, method: {}, URI: {}",
                    httpExchange.getRemoteAddress().getAddress().toString(),
                    httpExchange.getRequestMethod(),
                    httpExchange.getRequestURI().toString());

            graphsMap = SQLHelper.readGraphsMap();
        } catch (SQLException e) {
            LOGGER.warn("Failed to load DB data", e);
            makeResponse(e.getMessage().getBytes(), httpExchange, 500);
        }
        String requestMethod = httpExchange.getRequestMethod();

        //processing POST request
        if (requestMethod.equals("POST")) {
            executorService.execute(() -> {
                try {
                    //getting ip address
                    String ip = httpExchange.getRemoteAddress().getAddress().toString().replaceFirst("/", "");

                    //getting user-agent info
                    List<String> userAgent = httpExchange.getRequestHeaders().get("User-agent");
                    StringJoiner userAgentJoiner = new StringJoiner(",");
                    userAgent.forEach(userAgentJoiner::add);

                    //making graph record
                    String currentId = createId();
                    GraphItem graphItem = new GraphItem(httpExchange.getRequestBody(), ip, userAgentJoiner.toString(), currentId);
                    SQLHelper.addGraph(graphItem);
                    graphsMap.put(currentId, graphItem);

                    //making response
                    httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    makeResponse(currentId.getBytes(), httpExchange, 200);

                } catch (CantCastJSONException e) {
                    LOGGER.warn("Failed to add a  new graph ", e);
                    makeResponse(e.getMessage().getBytes(), httpExchange, 422);
                } catch (SQLException | IOException e) {
                    LOGGER.warn("Failed to add a  new graph ", e);
                    makeResponse(e.getMessage().getBytes(), httpExchange, 500);
                }
            });
        }

        //processing GET request
        else if (requestMethod.equals("GET")) {
            executorService.execute(() -> {
                try {
                    String[] request = httpExchange.getRequestURI().getPath().substring(1).split("/");
                    if (request.length == 0 || !CORRECT_REQUESTS.containsKey(request[0])) {
                        makeResponse(("Wrong request.\n" + REQUEST_DESCRIPTION).getBytes(),
                                httpExchange, 400);
                        LOGGER.info("Got a wrong GET request");
                        return;
                    }
                    String requestType = request[0];

                    //processing getGraph request
                    if (requestType.equals("getGraph")) {
                        if (request.length != 2) {
                            makeResponse(("Need graph ID.\n" + REQUEST_DESCRIPTION).getBytes(),
                                    httpExchange, 400);
                            LOGGER.info("Got a GET request with no graph ID");
                        }
                        else if (!graphsMap.containsKey(request[1])) {
                            makeResponse("No such a graph. Check ID\n".getBytes(), httpExchange, 404);
                            LOGGER.info("Got a GET request with wrong graph ID");
                        }
                        else {
                            makeResponse(graphsMap.get(request[1]).getGraph(), httpExchange, 200);
                            LOGGER.info("Got a correct GET request, response was sent");
                        }
                    }

                } catch (Exception e) {
                    LOGGER.warn("Error request processing");
                    makeResponse("Unknown error. Check logs.\n".getBytes(), httpExchange, 500);
                }
            });
        }

        else if (requestMethod.equals("OPTIONS")) {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "*");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            httpExchange.sendResponseHeaders(204, -1);
            LOGGER.debug("OPTIONS response was sent");
        }

        //processing unsupported request
        else {
            makeResponse("Unsupported method. POST and GET only.\n".getBytes(), httpExchange, 405);
            LOGGER.info("Unsupported request");
        }
    }

    public synchronized void makeResponse(byte[] response, HttpExchange httpExchange, int code) {
        try {
            httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            httpExchange.sendResponseHeaders(code, response.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(response);
            out.close();
        } catch (IOException e) {
            LOGGER.warn("Can't make a response ", e);
        }
    }

    private synchronized String createId() {
        String id;
        do {
            id = String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        } while (graphsMap.containsKey(id));
        LOGGER.debug("new ID {} was created", id);
        return id;
    }
}
