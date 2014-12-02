package com.wifindus.meshtester.threads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.Static;
import com.wifindus.meshtester.logs.Logger;

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
    private static String versionString;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public UpdateThread(Context launchingContext)
    {
        super(launchingContext);
        try {
            versionString = MeshApplication.systems().getPackageManager()
                    .getPackageInfo(launchingContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionString = "NULL";
        }

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
        return 1000;
    }

    @Override
    protected void prepare()
    {
        Logger.i(this, "Initializing updater thread...");
        try
        {
            updateSocket = new DatagramSocket();
            updateSocket.setBroadcast(true);
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
        if ((time - MeshApplication.lastCleaned()) >= 5000)
            MeshApplication.forceDirty();

        if (!MeshApplication.isDirty())
            return;

        //generate message content
        String message = "EYE|DEV|" + Long.toHexString(MeshApplication.getID()).toUpperCase()
            + "|" + Long.toHexString(time).toUpperCase()
			+ "|dt:" + MeshApplication.getDeviceType()
            + "|user:" + MeshApplication.getUserID()
            + "|ver:" + versionString
            + "|sdk:" + android.os.Build.VERSION.SDK_INT;
        Location loc = MeshApplication.getLocation();
		if (loc != null)
		{
            message += "|lat:" + Static.locationFormat.format(loc.getLatitude());
            message += "|long:" + Static.locationFormat.format(loc.getLongitude());
            if (loc.hasAccuracy())
                message += "|acc:" + loc.getAccuracy();
            if (loc.hasAltitude())
                message += "|alt:" + loc.getAccuracy();
		}

        try
        {
            byte[] buf = message.getBytes();
            updateSocket.send(new DatagramPacket(buf, buf.length, InetAddress.getByName(WIFI_SERVER), WIFI_SERVER_PORT));
            MeshApplication.clean(logContext());
        }
        catch (UnknownHostException e)
        {
            Logger.e(this,"Update failed; %s unknown",WIFI_SERVER);
        }
        catch (SocketException e)
        {
            Logger.e(this, "Update failed; SocketException thrown", e.toString());
        }
        catch (IOException e)
        {
            Logger.e(this, "Update failed; %s", e.toString());
        }
    }

    @Override
    protected void cleanup()
    {

    }
}
