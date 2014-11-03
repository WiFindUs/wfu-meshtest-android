package com.wifindus.meshtester;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.wifindus.meshtester.logs.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
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
    public static final String ACTION_UPDATE_CLEANED = MeshApplication.ACTION_PREFIX + "UPDATE_CLEANED";
    public static final String ACTION_UPDATE_USER = MeshApplication.ACTION_PREFIX + "UPDATE_USER";

    private static volatile MeshService meshService = null;
    private static volatile SystemManager systemManager =  null;
    private static volatile Location location = null;
	private static volatile SharedPreferences preferences = null;

    //overall status
    private static volatile boolean dirty = true;
    private static volatile long lastCleanTime = 0;

    //device info
    private static volatile String hash = null;
    private static volatile int userID = -1;
    private static volatile long lastSignInTime = 0;
    private static volatile ConcurrentHashMap<Integer, String> userNames = new ConcurrentHashMap<Integer, String>();

    //mesh status
    private static volatile boolean meshConnected = false;
    private static volatile long meshConnectedSince = 0;
    private static volatile int meshAddress = 0;
    private static volatile InetAddress meshInetAddress = null;
    private static volatile String meshHostName = "";
    private static volatile int meshNodeNumber = 0;
    private static volatile String meshNodeHash = "";

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate()
    {
        super.onCreate();

        //strict Mode
        //StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
        //StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

        //structures
        if (systemManager == null)
            systemManager = new SystemManager(this);
        if (preferences == null)
            preferences = getSharedPreferences("com.wifindus.eye.sharedprefs", Context.MODE_PRIVATE);

        //hash
        SharedPreferences.Editor editor = preferences.edit();
        String tempHash = preferences.getString("hash", null);
        if (tempHash == null) {
            String seed = "abcdefghijklmnopqrstuvwxyzABCDEFHIJKLMNOPQRSTUVWXYZ0123456789";
            Random r = new Random();
            tempHash = "";
            for (int i = 0; i < 8; i++) {
                int idx = r.nextInt(seed.length());
                tempHash += seed.substring(idx, idx + 1);
            }
            editor.putString("hash", tempHash);
        }
        hash = tempHash;

        //currently signed in user
        userID = preferences.getInt("userID", -1);
        lastSignInTime = preferences.getLong("lastSignInTime", 0);
        if (userID > -1 && (System.currentTimeMillis() - lastSignInTime) > SIGNOUT_INVALID_TIME)
        {
            userID = -1;
            lastSignInTime = 0;
            editor.putInt("userID", userID);
            editor.putLong("lastSignInTime", lastSignInTime);
        }
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

    public static final String getHash()
    {
        return hash;
    }

    public static final Location getLocation()
    {
        return location;
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
        if (loc == null && location == null)
            return;

        boolean changed = false;
        if (loc != null && location != null)
        {
            if (!location.equals(loc))
                changed = true;
        }
        else
            changed = true;

        if (!changed)
            return;

        location = loc;
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
        else {
            meshInetAddress = null;
            meshHostName = "";
        }

        Static.broadcastSimpleIntent(context, ACTION_UPDATE_MESH_ADDRESS);
    }
}

