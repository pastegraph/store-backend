package timeToLive;

import java.util.Date;

public class Limited extends TimeToLive {

    private Date expirationTime;

    public Limited(Date downloadTime, int minToLive) {
        super(downloadTime);
        this.expirationTime = (Date) getDownloadTime().clone();
        expirationTime = new Date(getDownloadTime().getTime() + 60000L * minToLive);
    }

    public Limited(Date downloadTime, Date expirationTime) {
        super(downloadTime);
        this.expirationTime = expirationTime;
    }

    @Override
    public Date getExpirationTime() {
        return expirationTime;
    }

    @Override
    public boolean timeIsUp() {
        return new Date().after(expirationTime);
    }
}
