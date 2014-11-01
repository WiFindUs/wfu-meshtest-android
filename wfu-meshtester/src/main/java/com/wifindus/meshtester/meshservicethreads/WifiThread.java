package com.wifindus.meshtester.meshservicethreads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.logs.Logger;
import com.wifindus.meshtester.meshservicethreads.BaseThread;

import java.net.InetAddress;
import java.util.List;

/**
 * Created by marzer on 25/04/2014.
 */
public class WifiThread extends BaseThread
{
    private static final String TAG = WifiThread.class.getName();
    private volatile boolean hasWifi = false;
    private volatile NetworkInfo activeNetwork;
    private volatile WifiInfo wifiInfo;
    private volatile WifiManager wifiManager = null;
    private volatile ConnectivityManager connectivityManager = null;
    private volatile WifiConfiguration wifindus_public = null;
    private static final String WIFI_SSID = "\"wifindus_public\"";
    private static final String WIFI_PSK = "\"a8jFIVcag82H461\"";
    //private static final String WIFI_SSID = "\"Marzer WLAN\"";
   // private static final String WIFI_PSK = "\"omgwtflol87\"";
    private static final int STATE_NO_WIFI = -1;
    private static final int STATE_WIFI_WAITING = 0;
    private static final int STATE_WIFI_CONNECTING = 1;
    private static final int STATE_WIFI_OK = 2;

    private volatile int state = STATE_NO_WIFI;


    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public WifiThread(Context launchingContext)
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

    /////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    /////////////////////////////////////////////////////////////////////

    @Override
    protected long iterationInterval()
    {
        return 10000;
    }

    @Override
    protected void prepare()
    {
        Logger.i(this, "Initializing WiFi thread...");
        connectivityManager = systems().getConnectivityManager();
        hasWifi = systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
        if (!hasWifi)
        {
            Logger.e(this, "Missing WiFi packages!");
            cancelThread();
            return;
        }
        wifiManager = systems().getWifiManager();
        Logger.i(this, "WiFi thread OK.");
    }

    @Override
    protected void iteration() {
        //is wifi disabled?
        if (!wifiManager.isWifiEnabled()) {
            MeshApplication.updateMeshConnected(logContext(), false);
            wifiManager.setWifiEnabled(true);
            if (state != STATE_WIFI_WAITING) {
                Logger.w(this, "WiFi disabled, re-enabling...");
                state = STATE_WIFI_WAITING;
            }
            return; //wait for it to go around again
        }

        //are we connected to a network already?
        activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null
                || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI
                || (wifiInfo = wifiManager.getConnectionInfo()).getSSID().compareTo(WIFI_SSID) != 0
                )
        {
            MeshApplication.updateMeshConnected(logContext(), false);
            wifiManager.enableNetwork(getWifindusPublicID(), false);
            wifiManager.reconnect();

            if (state != STATE_WIFI_CONNECTING)
            {
                if (state == STATE_WIFI_OK)
                    Logger.w(this, "Connection to mesh lost!");
                Logger.i(this, "Connecting to wifindus_public...");
                state = STATE_WIFI_CONNECTING;
            }
        }
        else
        {
            MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
            MeshApplication.updateMeshConnected(logContext(), true);
            if (state != STATE_WIFI_OK)
            {
                Logger.i(this, "Connected to mesh OK.");
                state = STATE_WIFI_OK;
            }
        }
    }

    @Override
    protected void cleanup()
    {

    }

    /////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////////////////////////////////

    private int getWifindusPublicID()
    {
        //if we've not already seen it
        if (wifindus_public == null) {
            //look for it in the system list
            List<WifiConfiguration> items = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration item : items) {
                if (item.SSID.compareTo(WIFI_SSID) == 0) {
                    wifindus_public = item;
                    break;
                }
            }
        }

        //if it's still null, create it ourselves
        if (wifindus_public == null)
        {
            wifindus_public = new WifiConfiguration();
            wifindus_public.SSID = WIFI_SSID;
            wifindus_public.priority = 10000;
            wifindus_public.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifindus_public.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifindus_public.preSharedKey = WIFI_PSK;
            wifiManager.addNetwork(wifindus_public);
        }

        return wifindus_public.networkId;
    }
}
