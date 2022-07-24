package timeToLive;

import java.util.Calendar;
import java.util.Date;

public class Forever extends TimeToLive {


    public Forever(Date downloadTime) {
        super(downloadTime);
    }
    @Override
    public boolean timeIsUp() {
        return false;
    }

    @Override
    public Date getExpirationTime() {
        return new Date(0);
    }
}
