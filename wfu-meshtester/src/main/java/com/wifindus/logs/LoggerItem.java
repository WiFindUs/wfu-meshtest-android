package com.wifindus.logs;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by marzer on 26/04/2014.
 */
public class LoggerItem
{
    private static SimpleDateFormat format = new SimpleDateFormat("HH:mm");

	private int level;
    private String tag;
    private String msg;
	private Date timestamp;

    public LoggerItem(int level, String tag, String msg)
    {
        this.level = level;
        this.tag = tag;
        this.msg = msg;
		timestamp = new Date();
    }

    public int getLevel()
    {
        return level;
    }

    public String getTag()
    {
        return tag;
    }

    public String getMessage()
    {
        return msg;
    }

	public Date getTimestamp()
	{
		return timestamp;
	}

	public String getTimestampString()
	{
		return format.format(timestamp);
	}
}
