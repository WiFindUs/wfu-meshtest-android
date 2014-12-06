package com.wifindus.meshtester.threads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.wifindus.BaseThread;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.logs.Logger;

/**
 * Created by marzer on 25/04/2014.
 */
public class LocationThread extends BaseThread implements LocationListener
{
    private static final String TAG = LocationThread.class.getName();
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private boolean hasLocation = false;
    private boolean hasGPS = false;
    private Handler handler = null;
    private boolean ok = false;
    private volatile Location location = null;
	private float batt = 1.0f;
    private boolean hasRegistered = false;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public LocationThread(Context launchingContext)
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

    @Override
    public void onLocationChanged(Location location)
    {
        assessLocation(location);
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
    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }

    /////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    /////////////////////////////////////////////////////////////////////

    @Override
    public long timeoutLength()
    {
        float battery = MeshApplication.getBatteryPercentage();
		return (battery >= 0.75f || MeshApplication.isBatteryCharging()) ? 1000 :
			(battery >= 0.25 ? 2500 : 5000);
    }

    @Override
    protected void prepare()
    {
        Logger.i(this, "Initializing location thread...");

        handler = new Handler(Looper.getMainLooper());
        hasLocation = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION);
        hasGPS = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        if (!hasLocation || !hasGPS)
        {
            Logger.e(this, "Missing location packages!");
            cancelThread();
            return;
        }
		registerLocationUpdateListener();
        assessLocation(systems().getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER));
        ok = true;
        Logger.i(this, "Location thread OK.");
    }

    @Override
    protected void iteration()
    {
		assessLocation(systems().getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER));

		//check battery level shifts
		float newBatt = MeshApplication.getBatteryPercentage();
		if ((batt >= 0.75f && newBatt < 0.75f) || (batt >= 0.25f && newBatt < 0.25f))
			registerLocationUpdateListener();
		batt = newBatt;
    }

    @Override
    protected void cleanup()
    {
        if (!ok)
            return;

        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Logger.i(LocationThread.this, "Un-registering location updates");
                systems().getLocationManager().removeUpdates(LocationThread.this);
                hasRegistered = false;
            }
        });
    }

    //see: http://developer.android.com/guide/topics/location/strategies.html
    protected boolean isBetterLocation(Location location, Location currentBestLocation)
    {
        if (location == null)
            return false;

        if (currentBestLocation == null)
            return true;

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer)
            return true;
        else if (isSignificantlyOlder)
            return false;

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate)
            return true;
        else if (isNewer && !isLessAccurate)
            return true;
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
            return true;
        return false;
    }

	private void registerLocationUpdateListener()
	{
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (hasRegistered) {
                    Logger.i(LocationThread.this, "Un-registering previous location updates");
                    systems().getLocationManager().removeUpdates(LocationThread.this);
                }

                long tl = timeoutLength();
                Logger.i(LocationThread.this, "Registering location updates ("+tl+"ms)");

				Criteria criteria = new Criteria();
				criteria.setAccuracy(Criteria.ACCURACY_FINE);
				criteria.setAltitudeRequired(false);
				criteria.setBearingRequired(false);
				criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
				criteria.setCostAllowed(false);
				criteria.setPowerRequirement(Criteria.POWER_HIGH);

				systems().getLocationManager().requestLocationUpdates(
                        tl,
					5,
					criteria,
					LocationThread.this,
					null
				);
                hasRegistered = true;
			}
		});
	}

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2)
    {
        if (provider1 == null)
            return provider2 == null;
        return provider1.equals(provider2);
    }

    private void assessLocation(Location newLoc)
    {
        if (isBetterLocation(newLoc, location))
        {
            location = newLoc;
            MeshApplication.updateLocation(logContext(), new Location(location));
        }
    }
}
