import java.io.InputStream;
import java.util.*;

import Exceptions.CantCastJSONException;
import org.json.*;

public class GraphItem {

    private boolean visible;
    private final String graphBody, ip, id;
    private final String userAgent;

    private Date expirationTime;

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
            try {
                int minutesToLive = jsonObject.getInt("expirationMinutes");
                if (minutesToLive > 0)
                    expirationTime = new Date(System.currentTimeMillis() + minutesToLive * 60000L);
                else expirationTime = new Date(0);
            } catch (JSONException e) {
                expirationTime = new Date(0);
            }

            //reading graph body
            graphBody = jsonObject.getString("graphBody");
        } catch (Exception e) {
            throw new CantCastJSONException("Wrong input data. Check your JSON.");
        }
    }

    public GraphItem(boolean visible, Date expirationTime, String graphBody, String ip, String userAgent, String id) {
        this.visible = visible;
        this.expirationTime = expirationTime;
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

    public Date getExpirationTime() {
        return expirationTime;
    }
}
