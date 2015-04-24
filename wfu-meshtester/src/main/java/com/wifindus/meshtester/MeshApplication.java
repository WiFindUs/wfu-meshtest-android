package com.wifindus.meshtester;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.wifindus.DeviceID;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by marzer on 25/04/2014.
 */
public class MeshApplication extends Application implements LogSender
{
	public static final long SIGNOUT_INVALID_TIME = 1000 * 60 * 60 * 8; //eight hours

	public static final int SERVER_PRIMARY = 0;
	public static final int SERVER_SECONDARY = 1;
	public static final int SERVER_WEB = 2;
	public static final int SERVER_COUNT = 3;

	public static final String ACTION_PREFIX = "WFU_";
	public static final String ACTION_UPDATE_LOCATION = MeshApplication.ACTION_PREFIX + "UPDATE_LOCATION";
	public static final String ACTION_UPDATE_CONNECTION_STATE = MeshApplication.ACTION_PREFIX + "UPDATE_CONNECTION_STATE";
	public static final String ACTION_UPDATE_USER = MeshApplication.ACTION_PREFIX + "UPDATE_USER";
	public static final String ACTION_UPDATE_PINGS = MeshApplication.ACTION_PREFIX + "UPDATE_PINGS";
	public static final String ACTION_UPDATE_BATTERY = MeshApplication.ACTION_PREFIX + "UPDATE_BATTERY";
	public static final String ACTION_UPDATE_NODE = MeshApplication.ACTION_PREFIX + "UPDATE_NODE";

	private static final String TAG = MeshApplication.class.getName();
	private static volatile MeshService meshService = null;
	private static volatile SystemManager systemManager = null;
	private static volatile SharedPreferences preferences = null;
	private static volatile boolean autoScrollLog = true;

	//overall status
	private static final VolatilePropertyList properties
		= new VolatilePropertyList();

	//device info
	private static volatile DeviceID id = null;
	private static volatile long lastSignInTime = 0;
	private static volatile ConcurrentHashMap<Integer, String> userNames
		= new ConcurrentHashMap<Integer, String>();
	private static volatile long lastLocationTime = 0;
	private static volatile File persistentDirectory = null;

	//mesh status
	private static volatile boolean meshConnected = false;
	private static volatile long meshConnectedSince = 0;

	//network/server
	private static volatile MeshServer[] servers = new MeshServer[SERVER_COUNT];
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

		//persistent files
		persistentDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/meshtester");

		if (Static.isExternalStorageWritable())
		{
			try { persistentDirectory.mkdirs(); }
			catch (Exception e) { }
		}

		//device id
		id = new DeviceID(preferences, "meshtester");

		//device type
		properties.addProperty("dt", new VolatileProperty<String>(
			systems().getTelephonyManager() == null
				|| systems().getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_NONE ? "TAB" : "PHO",
			"%s",
			120000
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
		properties.addProperty("ver", new VolatileProperty<String>(versionString, "%s", 120000));

		//device android sdk level
		properties.addProperty(
			"sdk", new VolatileProperty<Integer>(android.os.Build.VERSION.SDK_INT, "%d", 120000));

		//servers
		servers[SERVER_PRIMARY] = new MeshServer("192.168.1.1",false, preferences, "primaryServer");
		servers[SERVER_SECONDARY] = new MeshServer("192.168.1.2",false, preferences, "secondaryServer");
		servers[SERVER_WEB] = new MeshServer("118.88.26.88",true, preferences, "webServer"); //wifindus.com
		servers[SERVER_WEB].setEnabled(false);

		//currently signed in user
		SharedPreferences.Editor editor = preferences.edit();
		int userID = 0;
		try
		{
			userID = preferences.getInt("userID", 0);
		}
		catch(ClassCastException e){ }
		lastSignInTime = preferences.getLong("lastSignInTime", 0);
		if (userID > 0 && (System.currentTimeMillis() - lastSignInTime) > SIGNOUT_INVALID_TIME)
		{
			editor.putLong("userID", userID = 0);
			editor.putLong("lastSignInTime", lastSignInTime = 0);
		}
		properties.addProperty("user", new VolatileProperty<Integer>(userID, "%X", 120000));

		//battery level and charge state
		properties.addProperty("batt", new FloatProperty(0.5f, "%.2f", 120000, 0.1f));
		properties.addProperty("chg", new VolatileProperty<Integer>(0, "%d", 120000));

		//device location
		properties.addProperty("gps", new VolatileProperty<Integer>(0, "%d", 120000));
		properties.addProperty("fix", new VolatileProperty<Integer>(0, "%d", 120000));
		properties.addProperty("lat", new DoubleProperty(null, "%.6f", 10000, 0.000001));
		properties.addProperty("long", new DoubleProperty(null, "%.6f", 10000, 0.000001));
		properties.addProperty("acc", new FloatProperty(null, "%.2f", 10000, 0.1f));
		properties.addProperty("alt", new DoubleProperty(null, "%.2f", 10000, 1.0));
		properties.addToBlacklist("fix","lat","long","acc","alt");

		//mesh state
		forceMeshConnection = false;//preferences.getBoolean("forceMeshConnection", true);
		properties.addProperty("node", new VolatileProperty<Integer>(0, "%d", 10000));

		editor.apply();
	}

	/////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	/////////////////////////////////////////////////////////////////////

	public String logTag()
	{
		return TAG;
	}

	public Context logContext()
	{
		return this;
	}

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

	public static final DeviceID getID()
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
		editor.apply();
	}

	public static final boolean getAutoScrollLog() { return autoScrollLog; }

	public static final void setAutoScrollLog(boolean auto)
	{
		if (auto == autoScrollLog)
			return;
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("autoScrollLog", autoScrollLog = auto);
		editor.apply();
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

	public static final MeshServer getServer(int serverIndex)
	{
		if (serverIndex < 0 || serverIndex >= servers.length)
			return null;
		return servers[serverIndex];
	}

	public static final void addSignalStrengthHistory(SignalStrengthData data)
	{
		signalStrengthHistory.add(new SignalStrengthReportItem(data,
			(Double)getVolatileProperty("lat"),
			(Double)getVolatileProperty("long")));
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

		if (Static.isExternalStorageWritable())
		{
			message = context.logContext().getResources().getString(R.string.external_storage_unavailable);
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
			File dir = new File(persistentDirectory.getAbsolutePath(),"/signal_strengths");
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

	public static final long getLocationTime()
	{
		return lastLocationTime;
	}

	public static final int getUserID()
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

	public static final Integer getMeshNode()
	{
		return getVolatileProperty("node");
	}

	public static final void updateMeshNode(LogSender context, int nodeNumber)
	{
		if ((Integer)getVolatileProperty("node") == nodeNumber)
			return;
		setVolatileProperty("node", nodeNumber);
		Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_NODE);
	}

	public static final long getUserSignedInSince()
	{
		return lastSignInTime;
	}

	public static final String getUserName()
	{
		Integer userID = getUserID();
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

	public static final void updateGPSEnabled(LogSender context, boolean gpsEnabled)
	{
		if (((Integer)getVolatileProperty("gps") == 1) == gpsEnabled)
			return;
		setVolatileProperty("gps",gpsEnabled ? 1 : 0);
		updateGPSBlacklist();
	}

	public static final void updateGPSHasFix(LogSender context, boolean gpsHasFix)
	{
		if (((Integer)getVolatileProperty("fix") == 1) == gpsHasFix)
			return;
		setVolatileProperty("fix",gpsHasFix ? 1 : 0);
		updateGPSBlacklist();
	}

	private static final void updateGPSBlacklist()
	{
		if ((Integer)getVolatileProperty("gps") == 1)
		{
			properties.removeFromBlacklist("fix");
			if ((Integer)getVolatileProperty("fix") == 1)
				properties.removeFromBlacklist("lat","long","acc","alt");
			else
				properties.addToBlacklist("lat","long","acc","alt");
		}
		else
			properties.addToBlacklist("fix","lat","long","acc","alt");
	}

    public static final void updateLocation(LogSender context, Location loc)
    {
        if (loc == null)
		{
			setVolatileProperty("lat",null);
			setVolatileProperty("long",null);
			setVolatileProperty("alt",null);
			setVolatileProperty("acc",1000.0);
			return;
		}

		if (!loc.hasAccuracy())
			return;

		Double latitude = getLatitude();
		Double longitude = getLongitude();
		Double distance = (latitude == null || longitude == null ? 0.0
			: Static.distanceTo(loc.getLatitude(),loc.getLongitude(),latitude,longitude));

		setVolatileProperty("lat",loc.getLatitude());
		setVolatileProperty("long",loc.getLongitude());
		setVolatileProperty("acc",loc.getAccuracy());
		setVolatileProperty("alt",loc.hasAltitude() ? loc.getAltitude() : null);

        lastLocationTime = SystemClock.elapsedRealtime();

        Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_LOCATION);
    }

    public static final void updateUser(LogSender context, int userID)
    {
		if ((userID = Math.max(userID,0)) == getUserID())
            return;
		setVolatileProperty("user",userID);
        lastSignInTime = userID > 0 ? System.currentTimeMillis() : 0;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("userID", userID);
        editor.putLong("lastSignInTime", lastSignInTime);
        editor.apply();

        Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_USER);
    }

    public static final void updateMeshConnected(LogSender context, boolean connected)
    {
        if (connected == meshConnected)
            return;
        meshConnected = connected;
        meshConnectedSince = connected ? SystemClock.elapsedRealtime() : 0;
        Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_CONNECTION_STATE);
    }

	public static void updateBatteryStats(LogSender context, float percentage, boolean charging)
	{
		if ((int)(percentage * 100.0f) == (int)(getBatteryPercentage() * 100.0f)
			&& isBatteryCharging() == charging)
			return;

		setVolatileProperty("batt", percentage );
		setVolatileProperty("chg", charging ? 1 : 0);

		Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_BATTERY);
	}

    public static void updateNodePing(LogSender context, int node, PingResult newResult)
    {
        PingResult currentResult = nodePings.get(Integer.valueOf(node));
        if (currentResult == null && newResult == null)
            return;
        nodePings.put(Integer.valueOf(node), newResult);
        Static.broadcastSimpleIntent(context.logContext(), ACTION_UPDATE_PINGS);
    }

    public static boolean isPingThreadRunning()
    {
        return pingThread != null;
    }

    public static void startPingThread(LogSender context, String nodeRange)
    {
        if (isPingThreadRunning())
            return;
        pingThread = new PingThread(context.logContext(),nodeRange);
        if (pingThread.getNodeCount() == 0)
            pingThread = null;
        else
        {
            nodePings.clear();
            for (int i = 0; i < pingThread.getNodeCount(); i++)
                nodePings.put(Integer.valueOf(pingThread.getNodeNumber(i)), PingResult.WAITING);
            pingThread.start();
            if (context != null)
                Static.broadcastSimpleIntent(context.logContext(), MeshApplication.ACTION_UPDATE_PINGS);
        }
    }

    public static void stopPingThread(LogSender context)
    {
        if (!isPingThreadRunning())
            return;
        pingThread.cancelThread();
        pingThread = null;
        if (context != null)
            Static.broadcastSimpleIntent(context.logContext(), MeshApplication.ACTION_UPDATE_PINGS);
    }

	public static String updatePacketPayload()
	{
		return properties.flushList(false,true,true);
	}

	public static String webPacketPayload()
	{
		return properties.flushList(true,true,false);
	}
}

