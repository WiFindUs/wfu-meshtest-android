package com.wifindus.meshtester.logs;

import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by marzer on 26/04/2014.
 */
public class ServiceLog
{
    public static final int LOG_VERBOSE = 5;
    public static final int LOG_DEBUG = 4;
    public static final int LOG_INFO = 3;
    public static final int LOG_WARNING = 2;
    public static final int LOG_ERROR = 1;
    public static final int LOG_NONE = 0;

    private volatile int maximumLogLevel = LOG_INFO;
    private volatile CopyOnWriteArrayList<ServiceLogItem> unsyncedItems = new CopyOnWriteArrayList<ServiceLogItem>();

    public void log(int level, String tag, String msg)
    {
        if (level <= LOG_NONE || level > LOG_VERBOSE || level > maximumLogLevel || tag == "" || msg == "")
            return;
        switch (level)
        {
            case LOG_ERROR: Log.e(tag,msg); break;
            case LOG_WARNING: Log.w(tag, msg); break;
            case LOG_INFO: Log.i(tag, msg); break;
            case LOG_DEBUG: Log.d(tag, msg); break;
            case LOG_VERBOSE: Log.v(tag, msg); break;
        }

        unsyncedItems.add(new ServiceLogItem(level, tag, msg));
    }

    public ArrayList<ServiceLogItem> flushLog()
    {
        ArrayList<ServiceLogItem> returnArray = new ArrayList<ServiceLogItem>();
        returnArray.addAll(unsyncedItems);
        unsyncedItems.clear();
        return returnArray;
    }
}
