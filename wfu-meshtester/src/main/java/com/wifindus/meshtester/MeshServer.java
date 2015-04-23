package com.wifindus.meshtester;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

/**
 * Created by marzer on 23/04/2015.
 */
public class MeshServer implements Closeable
{
	private final int PORT_FIRST = 33339;
	private final int PORT_LAST = 33345;
	private final int PORT_COUNT = (PORT_LAST - PORT_FIRST) + 1;

	private final String prefsKey;
	private final SharedPreferences prefs;
	private String hostName = "";
	private InetAddress hostAddress = null;
	private boolean enabled = true;
	private final boolean webServer;
	private DatagramSocket udpSocket;

	/////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	/////////////////////////////////////////////////////////////////////

	public MeshServer(String defaultHost, boolean webServer, SharedPreferences prefs, String prefsKey)
	{
		if (defaultHost == null || (defaultHost = defaultHost.trim()).length() == 0)
			throw new IllegalArgumentException("Argument 'defaultHost' cannot be null or blank.");
		this.prefsKey = prefsKey == null ? "" : prefsKey.trim();
		this.prefs = prefs;
		this.webServer = webServer;

		if (!this.webServer)
		{
			try
			{
				udpSocket = new DatagramSocket();
			}
			catch (Exception e)
			{
				Log.e("MeshServer","Exception creating DatagramSocket",e);
				udpSocket = null;
			}
		}

		String host = "";
		if (this.prefs != null && this.prefsKey.length() > 0)
		{
			try
			{
				enabled = this.prefs.getBoolean(this.prefsKey+"_enabled", true);
				if (!this.webServer)
					host = this.prefs.getString(this.prefsKey, "").trim();
			}
			catch (Exception e)
			{
				Log.e("MeshServer","Exception getting initial data from SharedPreferences", e);
				host = "";
			}
		}
		setHostName(host.length() > 0 ? host : defaultHost);

	}

	public MeshServer(String defaultHost, boolean webMode)
	{
		this(defaultHost,webMode,null,null);
	}

	public MeshServer(String defaultHost)
	{
		this(defaultHost,false);
	}

	/////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	/////////////////////////////////////////////////////////////////////

	public boolean setHostName(String host)
	{
		if (host == null || (host = host.trim()).length() == 0)
			return false;
		if (hostAddress != null && host.compareTo(hostName) == 0)
			return false;

		InetAddress address;
		try
		{
			address = InetAddress.getByName(host);
		}
		catch (Exception e)
		{
			Log.e("MeshServer","Exception creating InetAddress from hostname", e);
			return false;
		}

		hostName = host;
		hostAddress = address;
		if (!this.webServer && this.prefs != null && this.prefsKey.length() > 0)
		{
			try
			{
				SharedPreferences.Editor prefsEditor = prefs.edit();
				prefsEditor.putString(prefsKey, hostName);
				prefsEditor.apply();
			}
			catch (Exception e)
			{
				Log.e("MeshServer","Exception writing hostname to SharedPreferences", e);
			}
		}

		return true;
	}

	public String getHostName()
	{
		return hostName;
	}

	public InetAddress getAddress()
	{
		return hostAddress;
	}

	public boolean isWebServer()
	{
		return webServer;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		if (this.enabled == enabled)
			return;
		this.enabled = enabled;
		if (prefs != null && prefsKey.length() > 0)
		{
			try
			{
				SharedPreferences.Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean(prefsKey+"_enabled", enabled);
				prefsEditor.apply();
			}
			catch (Exception e)
			{
				Log.e("MeshServer","Exception writing enabled to SharedPreferences", e);
			}
		}
	}

	public boolean isReady()
	{
		return enabled
			&& hostName != null
			&& hostName.length() > 0
			&& hostAddress != null
			&& (webServer || udpSocket != null);
			//&& (webServer || (udpSocket != null && !udpSocket.isClosed() && udpSocket.isConnected()));
	}

	@Override
	public void close() throws IOException
	{
		if (udpSocket != null)
			udpSocket.close();
	}

	public boolean send(String message)
	{
		if (!isReady()
			|| message == null
			|| (message = message.trim()).length() == 0)
			return false;

		if (webServer)
		{
			return true;
		}
		else
		{
			try
			{
				byte[] buf = message.getBytes();
				udpSocket.send(new DatagramPacket(buf, buf.length, hostAddress,
					PORT_FIRST + (new Random()).nextInt(PORT_COUNT)));

				return true;
			}
			catch (Exception e)
			{
				Log.e("MeshServer","Exception sending DatagramPacket to server", e);
			}
		}
		return false;
	}
}
