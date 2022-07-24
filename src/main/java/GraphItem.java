import java.io.InputStream;
import java.util.*;

import Exceptions.CantCastJSONException;
import org.json.*;
import timeToLive.Forever;
import timeToLive.Limited;
import timeToLive.TimeToLive;

public class GraphItem {

    private boolean visible;
    private TimeToLive timeToLive;
    private final String graphBody, ip, id;
    private final List<String> userAgent;

    public GraphItem(InputStream inputStream, String ip, List<String> userAgent, String id) throws CantCastJSONException {
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

            //reading expiration and download date
            Date currentTime = new Date();
            try {
                int minutesToLive = jsonObject.getInt("expirationMinutes");
                timeToLive = minutesToLive > 0 ? new Limited(currentTime, minutesToLive) : new Forever(currentTime);
            } catch (JSONException e) {
                timeToLive = new Forever(currentTime);
            }

            //reading graph body
            graphBody = jsonObject.getString("graphBody");
        } catch (Exception e) {
            throw new CantCastJSONException("Wrong input data. Check your JSON.");
        }
    }

    public GraphItem(boolean visible, TimeToLive timeToLive, String graphBody, String ip, List<String> userAgent, String id) {
        this.visible = visible;
        this.timeToLive = timeToLive;
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

    public List<String> getUserAgent() {
        return userAgent;
    }

    public String getId() {
        return id;
    }

    public byte[] getGraph() {
        return graphBody.getBytes();
    }

    public TimeToLive getTimeToLive() {
        return timeToLive;
    }
}
