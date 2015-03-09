package com.wifindus.meshtester;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.wifindus.PingResult;
import com.wifindus.Static;
import com.wifindus.logs.LogSender;
import com.wifindus.logs.Logger;
import com.wifindus.meshtester.threads.PingThread;
import com.wifindus.properties.DoubleProperty;
import com.wifindus.properties.FloatProperty;
import com.wifindus.properties.VolatileProperty;
import com.wifindus.properties.VolatilePropertyList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by marzer on 25/04/2014.
 */
public class MeshApplication extends Application
{
	public static final long SIGNOUT_INVALID_TIME = 1000 * 60 * 60 * 8; //eight hours

	public static final String ACTION_PREFIX = "WFU_";
	public static final String ACTION_UPDATE_LOCATION = MeshApplication.ACTION_PREFIX + "UPDATE_LOCATION";
	public static final String ACTION_UPDATE_MESH_ADDRESS = MeshApplication.ACTION_PREFIX + "UPDATE_MESH_ADDRESS";
	public static final String ACTION_UPDATE_CONNECTION_STATE = MeshApplication.ACTION_PREFIX + "UPDATE_CONNECTION_STATE";
	public static final String ACTION_UPDATE_USER = MeshApplication.ACTION_PREFIX + "UPDATE_USER";
	public static final String ACTION_UPDATE_PINGS = MeshApplication.ACTION_PREFIX + "UPDATE_PINGS";
	public static final String ACTION_UPDATE_BATTERY = MeshApplication.ACTION_PREFIX + "UPDATE_BATTERY";
	public static final String ACTION_UPDATE_SERVER = MeshApplication.ACTION_PREFIX + "UPDATE_SERVER";

	private static volatile MeshService meshService = null;
	private static volatile SystemManager systemManager = null;
	private static volatile SharedPreferences preferences = null;

	//overall status
	private static final VolatilePropertyList properties
		= new VolatilePropertyList();

	//device info
	private static volatile BigInteger id = null;
	private static volatile long lastSignInTime = 0;
	private static volatile ConcurrentHashMap<Long, String> userNames
		= new ConcurrentHashMap<Long, String>();
	private static volatile long lastLocationTime = 0;

	//mesh status
	private static volatile boolean meshConnected = false;
	private static volatile long meshConnectedSince = 0;
	private static volatile int meshAddress = 0;
	private static volatile InetAddress meshInetAddress = null;
	private static volatile String meshHostName = "";
	private static volatile int meshNodeNumber = 0;
	private static volatile long meshNodeID = -1;

	//network/server
	private static volatile String serverHostName = "";
	private static volatile int serverPort = -1;
	private static volatile InetAddress serverAddress = null;
	private static volatile boolean forceMeshConnection = true;
	private static volatile ArrayList<SignalStrengthReportItem> signalStrengthHistory
		= new ArrayList<SignalStrengthReportItem>();

	//pings
	private static volatile PingThread pingThread = null;
	private static volatile ConcurrentHashMap<Integer, PingResult> nodePings
		= new ConcurrentHashMap<Integer, PingResult>();

	/////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	/////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate()
	{
		super.onCreate();
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
		//StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
		//StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

		//structures
		if (systemManager == null)
			systemManager = new SystemManager(this);
		if (preferences == null)
			preferences = getSharedPreferences("com.wifindus.eye.sharedprefs", Context.MODE_PRIVATE);

		//device id
		id = new BigInteger(
			Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID).toUpperCase(), 16);

		//device type
		properties.addProperty("dt", new VolatileProperty<String>(
			systems().getTelephonyManager() == null
				|| systems().getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_NONE ? "TAB" : "PHO",
			"%s",
			30000
		));

		//device android version
		String versionString = "NULL";
		try
		{
			versionString = MeshApplication.systems().getPackageManager()
				.getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e)
		{
			versionString = "NULL";
		}
		properties.addProperty("ver", new VolatileProperty<String>(versionString, "%s", 30000));

		//device android sdk level
		properties.addProperty(
			"sdk", new VolatileProperty<Integer>(android.os.Build.VERSION.SDK_INT, "%d", 30000));

		//currently signed in user
		SharedPreferences.Editor editor = preferences.edit();
		long userID = 0;
		try
		{
			userID = preferences.getLong("userID", 0);
		}
		catch(ClassCastException e){ }
		lastSignInTime = preferences.getLong("lastSignInTime", 0);
		if (userID > 0 && (System.currentTimeMillis() - lastSignInTime) > SIGNOUT_INVALID_TIME)
		{
			editor.putLong("userID", userID = 0);
			editor.putLong("lastSignInTime", lastSignInTime = 0);
		}
		properties.addProperty(
			"user", new VolatileProperty<Long>(userID, "%X", 15000));

		//battery level and charge state
		properties.addProperty("batt", new FloatProperty(0.5f, "%.2f", 15000, 0.1f));
		properties.addProperty("chg", new VolatileProperty<Integer>(0, "%d", 15000));

		//device location
		properties.addProperty("lat", new DoubleProperty(null, "%.6f", 5000));
		properties.addProperty("long", new DoubleProperty(null, "%.6f", 5000));
		properties.addProperty("acc", new FloatProperty(null, "%.2f", 5000));
		properties.addProperty("alt", new DoubleProperty(null, "%.2f", 5000));

		//server IP address and port
		serverHostName = preferences.getString("serverHostName", "");
		if (serverHostName.length() == 0)
			editor.putString("serverHostName", serverHostName = "192.168.1.1");
		try
		{
			serverAddress = InetAddress.getByName(serverHostName);
		} catch (UnknownHostException e)
		{
			editor.putString("serverHostName", serverHostName = "192.168.1.1");
			try
			{
				serverAddress = InetAddress.getByName(serverHostName);
			} catch (UnknownHostException ex) { } //shouldn't fail
		}
		serverPort = preferences.getInt("serverPort", -1);
		if (serverPort < 0)
			editor.putInt("serverPort", serverPort = 33339);

		//force mesh usage
		forceMeshConnection = preferences.getBoolean("forceMeshConnection", true);

		editor.commit();
	}

	/////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	/////////////////////////////////////////////////////////////////////

	public static final void setMeshService(MeshService service)
	{
		if ((meshService == null && service != null) || (meshService != null && service == null))
			meshService = service;
	}

	public static final MeshService getMeshService()
	{
		return meshService;
	}

	public static final SystemManager systems() { return systemManager; }

	public static final BigInteger getID()
	{
		return id;
	}

	public static final boolean getForceMeshConnection() { return forceMeshConnection; }

	public static final void setForceMeshConnection(boolean force)
	{
		if (force == forceMeshConnection)
			return;
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("forceMeshConnection", forceMeshConnection = force);
		editor.commit();
	}

	public static final int getServerPort()
	{
		return serverPort;
	}

	public static final String getVersion()
	{
		return getVolatileProperty("ver");
	}

	public static final <T> T getVolatileProperty(String key)
	{
		return properties.<T>getProperty(key).getValue();
	}

	public static final <T> void setVolatileProperty(String key, T value)
	{
		properties.<T>getProperty(key).setValue(value);
	}

	public static final String getServerHostName()
	{
		return serverHostName;
	}

	public static final InetAddress getServerAddress()
	{
		return serverAddress;
	}

	public static final void addSignalStrengthHistory(SignalStrengthData data)
	{
		signalStrengthHistory.add(new SignalStrengthReportItem(data,
			(Double)getVolatileProperty("lat"),
			(Double)getVolatileProperty("long")));
	}

	public static final boolean setServer(Context context, String hostname, int port)
	{
		boolean changeHost = hostname != null && hostname.length() != 0
			&& hostname.compareTo(serverHostName) != 0;
		boolean changePort = port >= 1024 && port <= 65535 && port != serverPort;

		if (!changeHost && !changePort)
			return false;

		SharedPreferences.Editor editor = preferences.edit();

		if (changeHost)
		{
			InetAddress newAddress;
			try
			{
				newAddress = InetAddress.getByName(hostname);
			} catch (UnknownHostException ex)
			{
				return false;
			}

			editor.putString("serverHostName", serverHostName = hostname);
			serverAddress = newAddress;
		}

		if (changePort)
			editor.putInt("serverPort", serverPort = port);

		editor.commit();
		Static.broadcastSimpleIntent(context, ACTION_UPDATE_SERVER);
		return true;
	}

	public static final void exportSignalStrengthLog(LogSender context)
	{
		String message = "";

		if (signalStrengthHistory.size() == 0)
		{
			message = context.logContext().getResources().getString(R.string.signal_strengths_none);
			Toast.makeText(context.logContext(),
				message,
				Toast.LENGTH_LONG).show();
			Logger.w(context, message);
			return;
		}


		ArrayList<SignalStrengthReportItem> reportData = signalStrengthHistory;
		signalStrengthHistory = new ArrayList<SignalStrengthReportItem>();

		String filename = "signal_strengths_" + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())) + ".csv";

		try
		{
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File(sdCard.getAbsolutePath() + "/meshtester/signal_strengths");
			dir.mkdirs();
			File file = new File(dir, filename);


			FileOutputStream fOut = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(fOut);

			//headings
			osw.write(SignalStrengthReportItem.headers() + "\n");

			// data
			for (SignalStrengthReportItem item : reportData)
				osw.write(item.toString() + "\n");
			osw.flush();
			osw.close();
		} catch (Exception e)
		{
			message = context.logContext().getResources().getString(R.string.signal_strengths_saved_exception, filename, e.getClass().getName());
			Toast.makeText(context.logContext(),
				message,
				Toast.LENGTH_LONG).show();
			Logger.e(context, message);
			return;
		}

		message = context.logContext().getResources().getString(R.string.signal_strengths_saved_ok, filename);
		Toast.makeText(context.logContext(),
			message,
			Toast.LENGTH_LONG).show();
		Logger.i(context, message);
	}

	public static final Double getLatitude()
	{
		return getVolatileProperty("lat");
	}

	public static final Double getLongitude()
	{
		return getVolatileProperty("long");
	}

	public static final Float getAccuracy()
	{
		return getVolatileProperty("acc");
	}

	public static final Double getAltitude()
	{
		return getVolatileProperty("alt");
	}

	public static final long getLocationTime()
	{
		return lastLocationTime;
	}

	public static final long getUserID()
	{
		return getVolatileProperty("user");
	}

	public static final boolean isMeshConnected()
	{
		return meshConnected;
	}

	public static final long getMeshConnectedSince()
	{
		return meshConnectedSince;
	}

	public static final InetAddress getMeshAddress()
	{
		return meshInetAddress;
	}

	public static final String getMeshHostName()
	{
		return meshHostName;
	}

	public static final long getUserSignedInSince()
	{
		return lastSignInTime;
	}

	public static final String getDeviceType()
	{
		return getVolatileProperty("dt");
	}

	public static final String getUserName()
	{
		Long userID = getUserID();
		if (userID == null || userID <= 0)
			return "";
		String name = userNames.get(userID);
		return name == null ? "" : name;
	}

	public static final float getBatteryPercentage()
	{
		return getVolatileProperty("batt");
	}

	public static final boolean isBatteryCharging()
	{
		return (Integer)getVolatileProperty("chg") == 1;
	}

    public static final ConcurrentHashMap<Integer, PingResult> getNodePings()
    {
        return nodePings;
    }

    public static final void updateLocation(Context context, Location loc)
    {
        if (loc == null)
		{
			setVolatileProperty("lat",null);
			setVolatileProperty("long",null);
			setVolatileProperty("alt",null);
			setVolatileProperty("acc",null);
			return;
		}

		Double latitude = getLatitude();
		Double longitude = getLongitude();
		if (latitude != null && longitude != null
			&& Static.distanceTo(loc.getLatitude(),loc.getLongitude(),latitude,longitude) <= 0.5)
			return;

		setVolatileProperty("lat",loc.getLatitude());
		setVolatileProperty("long",loc.getLongitude());
		setVolatileProperty("alt",loc.hasAltitude() ? loc.getAltitude() : null);
		setVolatileProperty("acc",loc.hasAccuracy() ? loc.getAccuracy() : null);
        lastLocationTime = System.currentTimeMillis();

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_LOCATION);
    }

    public static final void updateUser(Context context, long userID)
    {
		if ((userID = Math.max(userID,0)) == getUserID())
            return;
		setVolatileProperty("user",userID);
        lastSignInTime = userID > 0 ? System.currentTimeMillis() : 0;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong("userID", userID);
        editor.putLong("lastSignInTime", lastSignInTime);
        editor.commit();

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_USER);
    }

    public static final void updateMeshConnected(Context context, boolean connected)
    {
        if (connected == meshConnected)
            return;
        meshConnected = connected;
        meshConnectedSince = connected ? System.currentTimeMillis() : 0;
        Static.broadcastSimpleIntent(context, ACTION_UPDATE_CONNECTION_STATE);
    }

    public static final void updateMeshAddress(Context context, int address)
    {
        if (meshAddress == address)
            return;

        meshAddress = address;

        if (meshAddress > 0)
        {
            try
            {
                meshInetAddress = InetAddress.getByName(String.format(
                        "%d.%d.%d.%d",
                        (meshAddress & 0xff),
                        (meshAddress >> 8 & 0xff),
                        (meshAddress >> 16 & 0xff),
                        (meshAddress >> 24 & 0xff)));
                meshHostName = meshInetAddress.getHostName();
            }
            catch (UnknownHostException e)
            {
                meshInetAddress = null;
                meshHostName = "";
            }
        }
        else
		{
            meshInetAddress = null;
            meshHostName = "";
        }

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_MESH_ADDRESS);
    }

	public static void updateBatteryStats(Context context, float percentage, boolean charging)
	{
		if ((int)(percentage * 100.0f) == (int)(getBatteryPercentage() * 100.0f)
			&& isBatteryCharging() == charging)
			return;

		setVolatileProperty("batt", percentage );
		setVolatileProperty("chg", charging ? 1 : 0);

		Static.broadcastSimpleIntent(context, ACTION_UPDATE_BATTERY);
	}

    public static void updateNodePing(Context context, int node, PingResult newResult)
    {
        PingResult currentResult = nodePings.get(Integer.valueOf(node));
        if (currentResult == null && newResult == null)
            return;
        nodePings.put(Integer.valueOf(node), newResult);
        Static.broadcastSimpleIntent(context, ACTION_UPDATE_PINGS);
    }

    public static boolean isPingThreadRunning()
    {
        return pingThread != null;
    }

    public static void startPingThread(Context context, String nodeRange)
    {
        if (isPingThreadRunning())
            return;
        pingThread = new PingThread(context,nodeRange);
        if (pingThread.getNodeCount() == 0)
            pingThread = null;
        else
        {
            nodePings.clear();
            for (int i = 0; i < pingThread.getNodeCount(); i++)
                nodePings.put(Integer.valueOf(pingThread.getNodeNumber(i)), PingResult.WAITING);
            pingThread.start();
            if (context != null)
                Static.broadcastSimpleIntent(context, MeshApplication.ACTION_UPDATE_PINGS);
        }
    }

    public static void stopPingThread(Context context)
    {
        if (!isPingThreadRunning())
            return;
        pingThread.cancelThread();
        pingThread = null;
        if (context != null)
            Static.broadcastSimpleIntent(context, MeshApplication.ACTION_UPDATE_PINGS);
    }

	public static String updatePacketPayload()
	{
		return properties.formattedValues(false,true,true);
	}
}

