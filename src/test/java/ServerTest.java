import com.github.pastegraph.store.Exceptions.ExceptionLogger;
import com.github.pastegraph.store.ServerMain;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServerTest {

    private HttpClient client = HttpClient.newHttpClient();
    private String graphID;
    String testJsonBody = """
                {
                    "isVisible": "True",
                    "graphBody": "TestGraphBody",
                    "expirationMinutes": "1"
                }
                """;

    @Before
    public void starsServerTest() {
        String tempSqlPath = "";
        try {
            client = HttpClient.newHttpClient();
            tempSqlPath = Files.createTempFile("pastegraph-temp-sql", ".s3db").toString();
        } catch (IOException e) {
            ExceptionLogger.log(e);
            System.exit(1);
        }
        ServerMain.main(tempSqlPath);
    }

    @Test
    public void mainTest() throws Exception {
        getNotExistsItem();
        postItem();
        getExistsItem();
        badPost();
    }

    private void getNotExistsItem() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/getGraph/helloRequest")).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assert response.statusCode() == 404: "Get not exists item failed!";
    }

    private void postItem() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/"))
                .POST(HttpRequest.BodyPublishers.ofString(testJsonBody))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assert response.statusCode() == 201: "Post item failed!";
        graphID = response.body();
    }

    private void getExistsItem() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/getGraph/" + graphID)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assert response.statusCode() == 200 && response.body().equals("TestGraphBody") : "GetExistsItem failed!";
    }

    private void badPost() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/"))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assert response.statusCode() == 422 : "Bad post failed!";
    }
}
