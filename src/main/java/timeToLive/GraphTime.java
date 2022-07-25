package timeToLive;

import java.util.Date;

public abstract class GraphTime {
    private final Date uploadTime;

    public GraphTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    public abstract boolean timeIsUp();

    public Date getUploadTime() {
        return uploadTime;
    }

    public abstract Date getExpirationTime();
}
