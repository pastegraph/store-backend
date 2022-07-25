import java.io.InputStream;
import java.util.*;

import Exceptions.CantCastJSONException;
import org.json.*;
import GraphTime.Forever;
import GraphTime.Limited;
import GraphTime.GraphTime;

public class GraphItem {

    private boolean visible;
    private GraphTime graphTime;
    private final String graphBody, ip, id;
    private final String userAgent;

    public GraphItem(InputStream inputStream, String ip, String userAgent, String id) throws CantCastJSONException {
        try {
            this.ip = ip;
            this.id = id;
            this.userAgent = userAgent;

            //reading json body
            Scanner scanner = new Scanner(inputStream);
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) builder.append(scanner.nextLine());
            JSONObject jsonObject = new JSONObject(builder.toString());

            //reading visibility
            try {
                visible = jsonObject.getBoolean("isVisible");
            } catch (JSONException e) {
                visible = true;
            }

            //reading expiration and upload date
            Date currentTime = new Date();
            try {
                int minutesToLive = jsonObject.getInt("expirationMinutes");
                graphTime = minutesToLive > 0 ?
                        new Limited(currentTime, new Date(currentTime.getTime() + 60000L * minutesToLive)) :
                        new Forever(currentTime);
            } catch (JSONException e) {
                graphTime = new Forever(currentTime);
            }

            //reading graph body
            graphBody = jsonObject.getString("graphBody");
        } catch (Exception e) {
            throw new CantCastJSONException("Wrong input data. Check your JSON.");
        }
    }

    public GraphItem(boolean visible, GraphTime graphTime, String graphBody, String ip, String userAgent, String id) {
        this.visible = visible;
        this.graphTime = graphTime;
        this.graphBody = graphBody;
        this.ip = ip;
        this.userAgent = userAgent;
        this.id = id;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getGraphBody() {
        return graphBody;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getId() {
        return id;
    }

    public byte[] getGraph() {
        return graphBody.getBytes();
    }

    public GraphTime getTimeToLive() {
        return graphTime;
    }
}
