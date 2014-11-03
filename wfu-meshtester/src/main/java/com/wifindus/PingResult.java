package com.wifindus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marzer on 3/11/2014.
 */
public class PingResult
{
    public static final Pattern PATTERN_PING_RESULTS
            = Pattern.compile("([0-9]{1,3})\\s+packets\\s+transmitted,"
            +"\\s*([0-9]{1,3})\\s+received,"
            +"(?:\\s*(?:\\+|-)?([0-9]{1,3})\\s+errors,)?"
            +"\\s*([0-9]{1,3}(?:[.][0-9]{1,6})?)%\\s+packet\\s+loss,"
            +"\\s*time\\s+[0-9]{1,5}(?:[.][0-9]{1,6})?\\s*ms"
            +"(?:\\s+rtt\\s*min/avg/max/mdev"
            +"\\s*=\\s*[0-9]{1,5}(?:[.][0-9]{1,6})?/([0-9]{1,5}"
            +"(?:[.][0-9]{1,6})?)/[0-9]{1,5}(?:[.][0-9]{1,6})?"
            +"/[0-9]{1,5}(?:[.][0-9]{1,6})?\\s*ms\\s*)?"

            ,Pattern.CASE_INSENSITIVE);

    public static final PingResult WAITING = new PingResult();

    public final int transmitted;
    public final int received;
    public final int errors;
    public final double loss;
    public final double averageTime;
    public final boolean error;
    public final boolean waiting;

    public PingResult(String pingOutput)
    {
        Matcher m = PATTERN_PING_RESULTS.matcher(pingOutput);
        error = !m.find();
        transmitted = !error && m.groupCount() > 0 ? Integer.parseInt(m.group(1)) : 0;
        received = !error && m.groupCount() > 1 ? Integer.parseInt(m.group(2)) : 0;
        errors = !error && m.groupCount() > 2 && m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        loss = !error && m.groupCount() > 3 ? Double.parseDouble(m.group(4)) : 0.0;
        averageTime = !error && m.groupCount() > 4 && m.group(5) != null? Double.parseDouble(m.group(5)) : 0.0;
        waiting = false;
    }

    private PingResult()
    {
        error = false;
        transmitted = received = errors = 0;
        loss = averageTime = 0.0;
        waiting = false;
    }
}
