package com.github.pastegraph.store;

import java.io.InputStream;
import java.util.*;

import com.github.pastegraph.store.Exceptions.CantCastJSONException;
import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphItem {

    private boolean visible;
    private final String graphBody, ip, id;
    private final String userAgent;

    private Date expirationTime, uploadTime;
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphItem.class);

    public GraphItem(InputStream inputStream, String ip, String userAgent, String id) throws CantCastJSONException {
        try {
            LOGGER.debug("GraphItem creating start");
            this.ip = ip;
            this.id = id;
            this.userAgent = userAgent;

            //reading json body
            Scanner scanner = new Scanner(inputStream);
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) builder.append(scanner.nextLine());
            JSONObject jsonObject = new JSONObject(builder.toString());
            LOGGER.debug("Request body parsed as String");

            //reading visibility
            try {
                visible = jsonObject.getBoolean("isVisible");
            } catch (JSONException e) {
                visible = true;
            }
            LOGGER.debug("Visibility initialized {}", visible);

            //reading expiration and upload date
            try {
                uploadTime = new Date();
                int minutesToLive = jsonObject.getInt("expirationMinutes");
                if (minutesToLive > 0)
                    expirationTime = new Date(System.currentTimeMillis() + minutesToLive * 60000L);
                else expirationTime = new Date(0);
            } catch (JSONException e) {
                expirationTime = new Date(0);
            }
            LOGGER.debug("Time data initialized. uploadTime: {}, expirationTime: {}", uploadTime.getTime(), expirationTime.getTime());

            //reading graph body
            graphBody = jsonObject.getString("graphBody");
        } catch (Exception e) {
            LOGGER.warn("Can't get graph data. ", e);
            throw new CantCastJSONException("Wrong input data. Check your JSON.");
        }
        LOGGER.debug("Graph Item initialized");
    }

    public GraphItem(boolean visible, Date expirationTime, Date uploadTime, String graphBody, String ip, String userAgent, String id) {
        this.uploadTime = uploadTime;
        this.visible = visible;
        this.expirationTime = expirationTime;
        this.graphBody = graphBody;
        this.ip = ip;
        this.userAgent = userAgent;
        this.id = id;
        LOGGER.debug("Graph Item {} was read from DB", id);
    }

    public boolean isVisible() {
        return visible;
    }

    public Date getUploadTime() {
        return uploadTime;
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
