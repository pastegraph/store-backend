import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    static final int port = 8080;

    public static void main(String[] args) {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        ExecutorService service = Executors.newFixedThreadPool(10);

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);


            server.createContext("/", httpExchange -> {
                switch (httpExchange.getRequestMethod()) {

                    case "POST" -> service.execute(() -> {
                        HashMap<String, String> tempMap = new HashMap<>(map);
                        Scanner scanner = new Scanner(httpExchange.getRequestBody());
                        while (scanner.hasNextLine()) {
                            String[] data;
                            try {
                                data = scanner.nextLine().split("=");
                            } catch (RuntimeException e) {
                                makeResponse("Wrong input data".getBytes(), httpExchange, 400);
                                return;
                            }
                            String key = data[0], value = data[1];
                            if (tempMap.put(key, value) != null) {
                                makeResponse(("Can't add item '" + key + "'. It already exists\n").getBytes(),
                                        httpExchange,
                                        500);
                                return;
                            }
                            map.putAll(tempMap);
                            makeResponse("Items added successfully\n".getBytes(), httpExchange, 200);
                        }
                    });

                    case "GET" -> service.execute(() -> {
                        String key = httpExchange.getRequestURI().toString().replaceFirst("/", "");
                        if (!map.containsKey(key)) {
                            makeResponse("No such an item\n".getBytes(), httpExchange, 400);
                            return;
                        }
                        makeResponse((map.get(key) + "\n").getBytes(), httpExchange, 200);
                    });

                    default -> makeResponse("Unknown response\n".getBytes(), httpExchange, 400);
                }
            });
            server.start();
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
    }

    private static void makeResponse(byte[] response, HttpExchange httpExchange, int code) {
        try {
            httpExchange.getResponseHeaders().add("Content/type", "text/plain; charset=UTF-8");
            httpExchange.sendResponseHeaders(code, response.length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(response);
            out.close();
        } catch (IOException e) {
            System.err.println("Connection to " + httpExchange.getRemoteAddress() + " failed");
        }

    }
}
