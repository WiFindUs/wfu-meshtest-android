package com.wifindus.meshtester.meshservicethreads;

import android.content.pm.PackageManager;

import com.wifindus.meshtester.meshservicethreads.BaseThread;

/**
 * Created by marzer on 25/04/2014.
 */
public class LocationThread extends BaseThread
{
    private volatile boolean hasLocation = false;
    private volatile boolean hasGPS = false;
    private volatile boolean hasNetworkLocation = false;

    @Override
    protected long iterationInterval()
    {
        if (!hasLocation || (!hasGPS && !hasNetworkLocation))
            return 60000;
        else if (!hasGPS)
            return 5000;
        else if (!hasNetworkLocation)
            return 2000;
        return 1000;
    }

    @Override
    protected void prepare()
    {
        hasLocation = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION);
        hasGPS = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        hasNetworkLocation = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
    }

    @Override
    protected void iteration()
    {
        if (!hasLocation || (!hasGPS && !hasNetworkLocation))
            return;
    }

    @Override
    protected void cleanup()
    {

    }
}
