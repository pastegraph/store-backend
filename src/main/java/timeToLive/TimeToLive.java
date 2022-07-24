package timeToLive;

import java.util.Calendar;
import java.util.Date;

public abstract class TimeToLive {
    private final Date downloadTime;

    public TimeToLive(Date downloadTime) {
        this.downloadTime = downloadTime;
    }

    public abstract boolean timeIsUp();

    public Date getDownloadTime() {
        return downloadTime;
    }

    public abstract Date getExpirationTime();
}
