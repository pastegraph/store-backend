package GraphTime;

import java.util.Date;

public class Forever extends GraphTime {


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
