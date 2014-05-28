package com.wifindus.meshtester;

import android.app.Application;

/**
 * Created by marzer on 25/04/2014.
 */
public class MeshApplication extends Application
{
    private static volatile MeshApplication appRef = null;
    private volatile MeshService meshService = null;
    private volatile SystemManager systemManager =  null;

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
        {
            appRef = this;
            systemManager = new SystemManager(this);
        }
    }

    public static MeshApplication ref()
    {
        return appRef;
    }

    public SystemManager systems()
    {
        return systemManager;
    }
}
