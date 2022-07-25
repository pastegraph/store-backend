import Exceptions.CantCastJSONException;
import Exceptions.ExceptionLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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

    {
        CORRECT_REQUESTS.put("getGraph", " - get graph body by ID. Example: /getGraph/12345\n");
        CORRECT_REQUESTS.put("getAllGraphs", " - get all graphs IDs\n");

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : CORRECT_REQUESTS.entrySet())
            builder.append(entry.getKey()).append(entry.getValue());
        REQUEST_DESCRIPTION = builder.toString();
    }
    private final ConcurrentHashMap<String, GraphItem> graphsMap;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Handler(ConcurrentHashMap<String, GraphItem> graphsMap) {
        this.graphsMap = graphsMap;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
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
                    graphsMap.put(currentId, graphItem);
                    SQLHelper.addGraph(graphItem);

                    //making response
                    makeResponse(currentId.getBytes(), httpExchange, 201);

                } catch (CantCastJSONException e) {
                    ExceptionLogger.log(e);
                    makeResponse(e.getMessage().getBytes(), httpExchange, 422);
                } catch (SQLException | IOException e) {
                    ExceptionLogger.log(e);
                    makeResponse(e.getMessage().getBytes(), httpExchange, 500);
                    System.exit(1);
                }
            });
        }

        //processing GET request
        else if (requestMethod.equals("GET")) {
            executorService.execute(() -> {
                try {
                    String[] request = httpExchange.getRequestURI().getPath().substring(1).split("/");
                    if (request.length == 0 || !CORRECT_REQUESTS.containsKey(request[0])) {
                        makeResponse(
                                ("Wrong request.\n" + REQUEST_DESCRIPTION).getBytes(),
                                httpExchange, 400);
                        return;
                    }
                    String requestType = request[0];

                    //processing getGraph request
                    if (requestType.equals("getGraph")) {
                        if (request.length != 2)
                            makeResponse(
                                    ("Need graph ID.\n" + REQUEST_DESCRIPTION).getBytes(),
                                    httpExchange, 400);
                        else if (!graphsMap.containsKey(request[1]))
                            makeResponse("No such a graph. Check ID\n".getBytes(), httpExchange, 404);
                        else makeResponse(graphsMap.get(request[1]).getGraph(), httpExchange, 200);
                    }

                } catch (Exception e) {
                    ExceptionLogger.log(e);
                    makeResponse("Unknown error. Check logs.\n".getBytes(), httpExchange, 500);
                }
            });
        }

        //processing unsupported request
        else {
            makeResponse("Unsupported method. POST and GET only.\n".getBytes(), httpExchange, 405);
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
            System.err.println("Connection to " + httpExchange.getRemoteAddress() + " failed");
        }
    }

    private synchronized String createId() {
        String id;
        do {
            id = String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        } while (graphsMap.containsKey(id));
        return id;
    }
}
