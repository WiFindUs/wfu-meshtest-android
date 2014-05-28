package com.wifindus.meshtester;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

/**
 * Created by marzer on 25/04/2014.
 */
public class SystemManager
{
    private Object[] Managers = new Object[7];

    public SystemManager(ContextWrapper host)
    {
        if (host == null)
            return;

        Managers[0] = host.getPackageManager();
        Managers[1] = (LocationManager)host.getSystemService(Context.LOCATION_SERVICE);
        Managers[2] = (ConnectivityManager)host.getSystemService(Context.CONNECTIVITY_SERVICE);
        Managers[3] = (WifiManager)host.getSystemService(Context.WIFI_SERVICE);
        Managers[4] = (TelephonyManager)host.getSystemService(Context.TELEPHONY_SERVICE);
        Managers[5] = (PowerManager)host.getSystemService(Context.POWER_SERVICE);

        try {
            Managers[6] = getPackageManager().getPackageInfo(host.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            android.util.Log.e(getClass().getSimpleName(), e.getClass().getName() + " thrown while initializing PackageInfo.");
        }
    }

    public PackageManager getPackageManager() {
        return (PackageManager)Managers[0];
    }

    public LocationManager getLocationManager() {
        return (LocationManager)Managers[1];
    }

    public ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager)Managers[2];
    }

    public WifiManager getWifiManager() {
        return (WifiManager)Managers[3];
    }

    public TelephonyManager getTelephonyManager() {
        return (TelephonyManager)Managers[4];
    }

    public PowerManager getPowerManager() {
        return (PowerManager)Managers[5];
    }

    public PackageInfo getPackageInfo() {
        return (PackageInfo)Managers[6];
    }
}
