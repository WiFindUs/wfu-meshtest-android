package com.wifindus.meshtester;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import java.util.Random;

/**
 * Created by marzer on 25/04/2014.
 */
public class MeshApplication extends Application
{
    private static volatile MeshApplication appRef = null;
    private volatile MeshService meshService = null;
    private volatile SystemManager systemManager =  null;
    private volatile Location lastLocation = null;
	private volatile SharedPreferences preferences = null;
    private String hash;

    public void setMeshService(MeshService service)
    {
        if ((meshService == null && service != null) || (meshService != null && service == null))
            meshService = service;
    }

    public MeshService getMeshService()
    {
        return meshService;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        //strict Mode
        //StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
        //StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

        if (appRef == null)
            appRef = this;

        systemManager = new SystemManager(this);
        preferences = getSharedPreferences("com.wifindus.eye.sharedprefs", Context.MODE_PRIVATE);
        String tempHash = preferences.getString("hash", null);
        if (tempHash == null)
        {
            String seed = "abcdefghijklmnopqrstuvwxyzABCDEFHIJKLMNOPQRSTUVWXYZ0123456789";
            Random r = new Random();
            tempHash = "";
            for (int i = 0; i < 8; i++)
            {
                int idx = r.nextInt(seed.length());
                tempHash += seed.substring(idx, idx + 1);
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("hash", tempHash);
            editor.commit();
        }
        hash = tempHash;
    }

    public static MeshApplication ref()
    {
        return appRef;
    }

    public SystemManager systems()
    {
        return systemManager;
    }

    public String getHash()
    {
        return hash;
    }

    public Location getLastLocation()
    {
        return lastLocation;
    }

    public void updateLastLocation(Location loc)
    {
        if (loc == null)
            return;
        lastLocation = loc;
    }
}
