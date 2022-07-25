package timeToLive;

import java.util.Date;

public class Limited extends GraphTime {

    private final Date expirationTime;

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
