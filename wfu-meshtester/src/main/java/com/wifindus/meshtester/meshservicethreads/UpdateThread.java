package com.wifindus.meshtester.meshservicethreads;

import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import com.wifindus.meshtester.MeshApplication;
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
    private volatile DatagramSocket updateSocket;
    public static final String WIFI_SERVER = "192.168.1.2";
    public static final int WIFI_SERVER_PORT = 33339;

    @Override
    protected long iterationInterval()
    {
        return 5000;
    }

    @Override
    protected void prepare()
    {
        try
        {
            updateSocket = new DatagramSocket(WIFI_SERVER_PORT);
        }
        catch (SocketException e)
        {
            updateSocket = null;
            e.printStackTrace();
        }
    }

    @Override
    protected void iteration()
    {
        if (updateSocket == null)
            return;

        //generate message content
        String message = "hash:" + MeshApplication.ref().getHash()
			+ "|type:DEVICE"
			+ "|deviceType:PHO";
        Location loc = MeshApplication.ref().getLastLocation();
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
            Log.d("eye", "UDP message sent: " + message);
            updateSocket.send(new DatagramPacket(buf, buf.length, InetAddress.getByName(WIFI_SERVER), WIFI_SERVER_PORT));
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
