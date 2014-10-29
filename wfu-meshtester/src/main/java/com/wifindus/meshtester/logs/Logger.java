package com.wifindus.meshtester.logs;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.Static;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marzer on 26/04/2014.
 */
public class Logger
{
    public static final String ACTION_UPDATE_LOG = MeshApplication.ACTION_PREFIX + "UPDATE_LOG";
    public static final int LOG_ERROR = 2;
    public static final int LOG_WARNING = 1;
    public static final int LOG_INFO = 0;
    private static int minimumLevel = LOG_INFO;
    private static volatile ArrayList<LoggerItem> unsyncedItems = new ArrayList<LoggerItem>();

    public static final void ex(LogSender sender, Exception ex)
    {
        log(LOG_ERROR, sender, ex.toString());
    }

    public static final void e(LogSender sender, String msg, Object... args)
    {
        log(LOG_ERROR, sender, msg, args);
    }

    public static final void w(LogSender sender, String msg, Object... args)
    {
        log(LOG_WARNING, sender, msg, args);
    }

    public static final void i(LogSender sender, String msg, Object... args)
    {
        log(LOG_INFO, sender, msg, args);
    }

    public static final List<LoggerItem> flush()
    {
        ArrayList<LoggerItem> returnArray = new ArrayList<LoggerItem>();
        synchronized (unsyncedItems)
        {
            returnArray.addAll(unsyncedItems);
            unsyncedItems.clear();
        }
        return returnArray;
    }

    private static final void log(int level, LogSender sender, String msg, Object... args)
    {
        if (level < LOG_INFO || level > LOG_ERROR || level < minimumLevel)
            return;

        String tag = sender == null ? "" : sender.logTag();
        if(msg == null)
            msg = "";
        if (args != null && args.length > 0)
            msg = String.format(msg,args);
        switch (level)
        {
            case LOG_ERROR: Log.e(tag,msg); break;
            case LOG_WARNING: Log.w(tag, msg); break;
            case LOG_INFO: Log.i(tag, msg); break;
        }

        unsyncedItems.add(new LoggerItem(level, tag, msg));
        Static.broadcastSimpleIntent(sender.logContext(), ACTION_UPDATE_LOG);
    }
}
