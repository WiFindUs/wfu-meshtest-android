package com.wifindus.meshtester.meshservicethreads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.logs.Logger;
import com.wifindus.meshtester.meshservicethreads.BaseThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by marzer on 25/04/2014.
 */
public class UpdateThread extends BaseThread
{
    private static final String TAG = UpdateThread.class.getName();
    private volatile DatagramSocket updateSocket;
    private static final String WIFI_SERVER = "192.168.1.1";
    private static final int WIFI_SERVER_PORT = 33339;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public UpdateThread(Context launchingContext)
    {
        super(launchingContext);
    }

    /////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////////////////////////////////////////

    public String logTag()
    {
        return TAG;
    }

    /////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    /////////////////////////////////////////////////////////////////////

    @Override
    protected long iterationInterval()
    {
        return 3000;
    }

    @Override
    protected void prepare()
    {
        Logger.i(this, "Initializing updater thread...");
        try
        {
            updateSocket = new DatagramSocket();
            Logger.i(this, "Updater thread OK.");

        }
        catch (SocketException e)
        {
            updateSocket = null;
            Logger.e(this, "Error creating socket!");
            cancelThread();
        }
    }

    @Override
    protected void iteration()
    {
        if (!MeshApplication.isMeshConnected())
            return;

        long time = System.currentTimeMillis();
        if (!MeshApplication.isDirty() && (time - MeshApplication.lastCleaned()) < 30000)
            return;

        //generate message content
        String message = "hash:" + MeshApplication.getHash()
            + "|timestamp:" + time
			+ "|type:DEVICE"
			+ "|deviceType:PHO";
        Location loc = MeshApplication.getLocation();
		if (loc != null)
		{
            message += "|latitude:" + loc.getLatitude();
            message += "|longitude:" + loc.getLongitude();
            if (loc.hasAccuracy())
                message += "|accuracy:" + loc.getAccuracy();
            if (loc.hasAltitude())
                message += "|altitude:" + loc.getAccuracy();
		}

        try
        {
            byte[] buf = message.getBytes();
            updateSocket.send(new DatagramPacket(buf, buf.length, InetAddress.getByName(WIFI_SERVER), WIFI_SERVER_PORT));
            MeshApplication.clean(logContext());
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void cleanup()
    {

    }
}
