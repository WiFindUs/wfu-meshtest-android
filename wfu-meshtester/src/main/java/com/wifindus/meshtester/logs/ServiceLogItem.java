package com.wifindus.meshtester.logs;

/**
 * Created by marzer on 26/04/2014.
 */
public class ServiceLogItem
{
    private int level;
    private String tag;
    private String msg;

    public ServiceLogItem(int level, String tag, String msg)
    {
        this.level = level;
        this.tag = tag;
        this.msg = msg;
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
}
