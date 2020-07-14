package vn.com.acacy.cameralibrary.core;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTime {
    public static long getTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        return Long.parseLong(format.format(date));
    }
}
