package com.wifindus.meshtester.threads;

import android.content.Context;
import android.util.Log;

import com.wifindus.BaseThread;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.Static;
import com.wifindus.logs.Logger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

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
		//return !MeshApplication.isMeshConnected()
		//	|| MeshApplication.getServerAddress() == null ? 5000 : 1000;
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
        if (!MeshApplication.isMeshConnected() || MeshApplication.getServerAddress() == null)
            return;

		String payload = MeshApplication.updatePacketPayload();
        if (payload.length() == 0)
            return;
		payload = Static.PATTERN_TRAILING_ZEROES.matcher(payload).replaceAll(".0");
		payload = Static.PATTERN_FLOAT_ZERO.matcher(payload).replaceAll("0");

		//generate message content
		String message = "EYE{DEV|" + MeshApplication.getID().toString(16).toUpperCase()
			+ "|" + Long.toHexString(System.currentTimeMillis()).toUpperCase()
			+ "{" + payload + "}}";

		try
		{
            byte[] buf = message.getBytes();
            updateSocket.send(new DatagramPacket(buf, buf.length, MeshApplication.getServerAddress(), MeshApplication.getServerPort()));
        }
		catch (SocketException se)
		{
			Logger.e(this, "Updater: Server unreachable");
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
