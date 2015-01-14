package com.wifindus.meshtester;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Environment;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.wifindus.MathHelper;
import com.wifindus.PingResult;
import com.wifindus.logs.LogSender;
import com.wifindus.logs.Logger;
import com.wifindus.meshtester.threads.PingThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    public static final String ACTION_UPDATE_CLEANED = MeshApplication.ACTION_PREFIX + "UPDATE_CLEANED";
    public static final String ACTION_UPDATE_USER = MeshApplication.ACTION_PREFIX + "UPDATE_USER";
    public static final String ACTION_UPDATE_PINGS = MeshApplication.ACTION_PREFIX + "UPDATE_PINGS";
	public static final String ACTION_UPDATE_BATTERY = MeshApplication.ACTION_PREFIX + "UPDATE_BATTERY";
	public static final String ACTION_UPDATE_SERVER = MeshApplication.ACTION_PREFIX + "UPDATE_SERVER";

    private static volatile MeshService meshService = null;
    private static volatile SystemManager systemManager =  null;
	private static volatile SharedPreferences preferences = null;

    //overall status
    private static volatile boolean dirty = true;
    private static volatile long lastCleanTime = 0;
    private static volatile String versionString = "NUL";

    //device info
    private static volatile long id = -1;
    private static volatile int userID = -1;
    private static volatile long lastSignInTime = 0;
    private static volatile ConcurrentHashMap<Integer, String> userNames
        = new ConcurrentHashMap<Integer, String>();
	private static volatile float batteryPercentage = 0.5f;
	private static volatile boolean batteryCharging = false;
    private static volatile Location location = null;
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

        //strict Mode
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());
        //StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
        //StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

        //structures
        if (systemManager == null)
            systemManager = new SystemManager(this);
        if (preferences == null)
            preferences = getSharedPreferences("com.wifindus.eye.sharedprefs", Context.MODE_PRIVATE);

        //version info
        try {
            versionString = MeshApplication.systems().getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionString = "NULL";
        }

        //id
		SharedPreferences.Editor editor = preferences.edit();
		id = preferences.getLong("id",-1);
		if (id < 0)
			editor.putLong("id", id = (long)(0xFFFFFFFFL * new Random().nextDouble()));

        //currently signed in user
        userID = preferences.getInt("userID", -1);
        lastSignInTime = preferences.getLong("lastSignInTime", 0);
        if (userID > -1 && (System.currentTimeMillis() - lastSignInTime) > SIGNOUT_INVALID_TIME)
        {
            editor.putInt("userID", userID = -1);
            editor.putLong("lastSignInTime", lastSignInTime = 0);
        }

		//server IP address and port
		serverHostName = preferences.getString("serverHostName", "");
		if (serverHostName.length() == 0)
			editor.putString("serverHostName", serverHostName = "192.168.1.1");
		try
		{
			serverAddress = InetAddress.getByName(serverHostName);
		}
		catch(UnknownHostException e)
		{
			editor.putString("serverHostName", serverHostName = "192.168.1.1");
			try
			{
				serverAddress = InetAddress.getByName(serverHostName);
			}
			catch (UnknownHostException ex) { } //shouldn't fail
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

    public static final long getID()
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
        return versionString;
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
        signalStrengthHistory.add(new SignalStrengthReportItem(data, location));
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
			}
			catch (UnknownHostException ex)
			{
				return false;
			}

			editor.putString("serverHostName", serverHostName = hostname);
			serverAddress = newAddress;
		}

		if(changePort)
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
        signalStrengthHistory = new  ArrayList<SignalStrengthReportItem>();

        String filename = "signal_strengths_" + (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())) + ".csv";

        try
        {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/meshtester/signal_strengths");
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
        }
        catch (Exception e)
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
              message ,
            Toast.LENGTH_LONG).show();
        Logger.i(context, message);
    }

    public static final Location getLocation()
    {
        return location;
    }

    public static final long getLocationTime()
    {
        return lastLocationTime;
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

    public static final int getUserID() { return userID; }

    public static final long getUserSignedInSince()
    {
        return lastSignInTime;
    }

    public static final String getDeviceType()
    {
        if (systems().getTelephonyManager() == null
            || systems().getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_NONE)
            return "TAB";
        return "PHO";
    }

    public static final String getUserName()
    {
        if (userID <= -1)
            return "";
        String name = userNames.get(Integer.valueOf(userID));
        return name == null ? "" : name;
    }

	public static final float getBatteryPercentage() { return batteryPercentage; }

	public static final boolean isBatteryCharging() { return batteryCharging; }

    public static final ConcurrentHashMap<Integer, PingResult> getNodePings()
    {
        return nodePings;
    }

    public static final boolean isDirty()
    {
        return dirty;
    }

    public static final void forceDirty()
    {
        dirty = true;
    }

    public static final void clean(Context context)
    {
        if (!dirty)
            return;
        lastCleanTime = System.currentTimeMillis();
        dirty = false;
        Static.broadcastSimpleIntent(context, ACTION_UPDATE_CLEANED);
    }

    public static final long lastCleaned()
    {
        return lastCleanTime;
    }

    public static final void updateLocation(Context context, Location loc)
    {
        if (loc == location //same, or both null
			|| (loc != null && location != null && location.distanceTo(loc) <= 0.1)) //too close
            return;

        location = loc;
        lastLocationTime = System.currentTimeMillis();
        dirty = true;

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_LOCATION);
    }

    public static final void updateUser(Context context, int userID)
    {
        if ((userID = Math.max(userID,-1)) == MeshApplication.userID)
            return;
        MeshApplication.userID = userID;
        lastSignInTime = userID >= 0 ? System.currentTimeMillis() : 0;
        dirty = true;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("userID", userID);
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
        dirty = true;

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_CONNECTION_STATE);
    }

    public static final void updateMeshAddress(Context context, int address)
    {
        if (meshAddress == address)
            return;

        meshAddress = address;
        dirty = true;

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
		if ((int)(percentage * 100.0f) == (int)(batteryPercentage * 100.0f)
			&& batteryCharging == charging)
			return;

		batteryPercentage = percentage;
		batteryCharging = charging;
		dirty = true;

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
}

