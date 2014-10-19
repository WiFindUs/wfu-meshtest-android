package com.wifindus.meshtester.meshservicethreads;

import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.meshservicethreads.BaseThread;

/**
 * Created by marzer on 25/04/2014.
 */
public class LocationThread extends BaseThread implements LocationListener
{
    private volatile boolean hasLocation = false;
    private volatile boolean hasGPS = false;
    private volatile boolean hasNetworkLocation = false;
    private volatile Handler handler = null;

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
        Log.d("eye", "LocationThread.prepare()");

        handler = new Handler(Looper.getMainLooper());
        hasLocation = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION);
        hasGPS = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        hasNetworkLocation = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
        if (hasLocation && (hasGPS || hasNetworkLocation))
        {
            Log.d("eye", "LocationThread.prepare(): pushing task to handler");

            handler.post(new Runnable() {
                @Override
                public void run() {

                    Log.d("eye", "Location updates requested");

                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setAltitudeRequired(false);
                    criteria.setBearingRequired(false);
                    criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                    criteria.setCostAllowed(false);
                    criteria.setPowerRequirement(Criteria.POWER_HIGH);

                    MeshApplication.ref().systems().getLocationManager().requestLocationUpdates(
                            1000,
                            5,
                            criteria,
                            LocationThread.this,
                            null
                    );
                }
            });
        }
        else
            Log.e("eye", "Location update request failed; invalid packages!");
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
        if (hasLocation && (hasGPS || hasNetworkLocation))
            MeshApplication.ref().systems().getLocationManager().removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location)
    {
        Log.d("eye", "Location updated: " + location.toString());
        MeshApplication.ref().updateLastLocation(location);
    }

    @Override
    public void onProviderDisabled(String provider)
    {


    }

    @Override
    public void onProviderEnabled(String provider)
    {


    }

    @Override
    public  void onStatusChanged(String provider, int status, Bundle extras)
    {

    }
}
