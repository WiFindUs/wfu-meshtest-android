package com.wifindus.meshtester.threads;

import android.content.Context;

import com.wifindus.BaseThread;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.logs.Logger;
import com.wifindus.meshtester.MeshServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by marzer on 25/04/2014.
 */
public class UpdateThread extends BaseThread
{
    private static final String TAG = UpdateThread.class.getName();
	private static final Pattern PATTERN_POINT_ZERO
		= Pattern.compile("[.]0+[|]", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_TRAILING_ZEROES
		= Pattern.compile("00+[|]", Pattern.CASE_INSENSITIVE);
	private long lastWebPacket = 0;

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
		return 1000;
	}

    @Override
    protected void prepare()
    {
        Logger.i(this, "Initializing updater thread...");
    }

    @Override
    protected void iteration()
    {
        if (!MeshApplication.isMeshConnected())
            return;

		boolean webPacketDue = (System.currentTimeMillis() - lastWebPacket) >= 60000;
		ArrayList<MeshServer> readyMeshServers = new ArrayList<MeshServer>();
		ArrayList<MeshServer> readyWebServers = webPacketDue ? new ArrayList<MeshServer>() : null;
		for (int i = 0; i < MeshApplication.SERVER_COUNT; i++)
		{
			MeshServer server = MeshApplication.getServer(i);
			if (server.isReady())
			{
				if (webPacketDue && server.isWebServer())
					readyWebServers.add(server);
				else
					readyMeshServers.add(server);
			}
		}

		if (readyMeshServers.size() > 0)
		{
			String packet = createPacket(MeshApplication.updatePacketPayload());
			if (packet.length() > 0)
			{
				boolean sent = false;
				for (MeshServer server : readyMeshServers)
				{
					if (server.send(packet))
					{
						sent = true;
						break;
					}
				}
				if (!sent)
					Logger.e(this,"Local update sending failed!");
			}
		}

		if (webPacketDue && readyWebServers.size() > 0)
		{
			String packet = createPacket(MeshApplication.webPacketPayload());
			if (packet.length() > 0)
			{
				boolean sent = false;
				for (MeshServer server : readyWebServers)
				{
					if (server.send(packet))
					{
						lastWebPacket = System.currentTimeMillis();
						sent = true;
						break;
					}
				}
				if (!sent)
					Logger.e(this,"Web update sending failed!");
			}
		}
    }

    @Override
    protected void cleanup()
    {

    }

	private String createPacket(String payload)
	{
		if (payload == null || (payload = payload.trim()).length() == 0)
			return "";

		payload = PATTERN_TRAILING_ZEROES.matcher(payload).replaceAll("0|");
		payload = PATTERN_POINT_ZERO.matcher(payload).replaceAll("|");

		return String.format("EYE{DEV|%s|%s{%s}}",
			MeshApplication.getID().hex(),
			Long.toHexString(System.currentTimeMillis()).toUpperCase(),
			payload);
	}
}
