package com.wifindus.meshtester.threads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import com.wifindus.BaseThread;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.Static;
import com.wifindus.logs.Logger;

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
	public long timeoutLength()
	{
		float battery = MeshApplication.getBatteryPercentage();
		return (battery >= 0.75f || MeshApplication.isBatteryCharging()) ? 1000 :
			(battery >= 0.25 ? 2500 : 5000);
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
        if (!MeshApplication.isMeshConnected() || MeshApplication.getServerAddress() == null)
            return;

        long time = System.currentTimeMillis();
        if ((time - MeshApplication.lastCleaned()) >= 10000)
            MeshApplication.forceDirty();

        if (!MeshApplication.isDirty())
            return;

		//generate message content
		String message = "EYE|DEV|" + Long.toHexString(MeshApplication.getID()).toUpperCase()
			+ "|" + Long.toHexString(time).toUpperCase()
			+ "|dt:" + MeshApplication.getDeviceType()
			+ "|ver:" + MeshApplication.getVersion()
			+ "|sdk:" + android.os.Build.VERSION.SDK_INT
			+ "|user:" + (MeshApplication.getUserID() >= 0 ?
				Long.toHexString(MeshApplication.getUserID()).toUpperCase()
				: "-1")
			+ "|batt:" + Static.percentageFormat.format(MeshApplication.getBatteryPercentage())
			+ "|chg:" + (MeshApplication.isBatteryCharging() ? "1" : "0");
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
            updateSocket.send(new DatagramPacket(buf, buf.length, MeshApplication.getServerAddress(), MeshApplication.getServerPort()));
            MeshApplication.clean(logContext());
        }
        catch (Exception e)
        {
            Logger.e(this, "Updater: %s thrown; %s", e.getClass().getName(), e.getMessage());
        }
    }

    @Override
    protected void cleanup()
    {

    }
}
