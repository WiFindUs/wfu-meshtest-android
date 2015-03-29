package com.wifindus.meshtester.threads;

import android.content.Context;

import com.wifindus.BaseThread;
import com.wifindus.PingResult;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.Static;
import com.wifindus.logs.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marzer on 3/11/2014.
 */
public class PingThread extends BaseThread
{
    public static final Pattern PATTERN_NODE_RANGE
        = Pattern.compile("\\s*([0-9]{1,3})(?:\\s*-\\s*([0-9]{1,3}))?\\s*",Pattern.CASE_INSENSITIVE);
    private static final String TAG = PingThread.class.getName();
    private final List<Integer> nodes = new ArrayList<Integer>();

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public PingThread(Context launchingContext, String nodeRange)
    {
        super(launchingContext);
        if (nodeRange == null || (nodeRange = nodeRange.trim()).isEmpty()) {
            return;
        }
        String[] ranges = nodeRange.trim().split("\\s*,\\s*");
        for (int i = 0; i < ranges.length; i++)
        {
            Matcher match = PATTERN_NODE_RANGE.matcher(ranges[i]);
            if (!match.matches())
                continue;
            int first = Integer.parseInt(match.group(1));
            String lastGroup = match.group(2);
            int last = lastGroup == null || lastGroup.isEmpty() ? first :  Integer.parseInt(lastGroup);
            if (first <= 0 || last <= 0 || first >= 255 || last >= 255 || first > last)
                continue;
            for (int j = first; j <= last; j++)
                nodes.add(Integer.valueOf(j));
        }
    }

    /////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////////////////////////////////////////

    public final int getNodeCount()
    {
        return nodes.size();
    }

    public final int getNodeNumber(int index)
    {
        return nodes.get(index);
    }

    @Override
    public long timeoutLength()
    {
        return 200;
    }

    @Override
    protected void prepare()
    {
        if (nodes.size() == 0)
        {
            cancelThread();
            return;
        }
    }

    @Override
    protected void iteration()
    {
        for (int i = 0; i < nodes.size(); i++)
        {
            String addr = "10.1.0."+nodes.get(i);
            String result = "";
            try
            {
                result = Static.ping(addr, 10);
                MeshApplication.updateNodePing(this,
                    nodes.get(i).intValue(),
                    new PingResult(result));
            }
            catch (IOException e)
            {
                Logger.e(this, "IOException thrown pinging %s", addr);
            }

            if (isCancelled())
                break;
        }
    }

    @Override
    protected void cleanup() {

    }

    @Override
    public String logTag() {
        return null;
    }
}
