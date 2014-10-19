package com.wifindus.meshtester.meshservicethreads;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import com.wifindus.meshtester.meshservicethreads.BaseThread;

import java.util.List;

/**
 * Created by marzer on 25/04/2014.
 */
public class WifiThread extends BaseThread
{
    private volatile boolean hasWifi = false;
    private volatile NetworkInfo activeNetwork;
    private volatile WifiInfo wifiInfo;
    private volatile WifiManager wifiManager = null;
    private volatile ConnectivityManager connectivityManager = null;
    public static final String WIFI_SSID = "\"Marzer WLAN\""; //"\"wifindus_public\""
    private static final String WIFI_PSK = "\"omgwtflol87\""; //"\"a8jFIVcag82H461\""

    @Override
    protected long iterationInterval()
    {
        if (!hasWifi)
            return 60000;
        return 5000;
    }

    @Override
    protected void prepare()
    {
        connectivityManager = app().systems().getConnectivityManager();
        hasWifi = app().systems().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
        if (hasWifi)
            wifiManager = app().systems().getWifiManager();
    }

    @Override
    protected void iteration()
    {
        if (!hasWifi)
            return;

        //is wifi disabled?
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);

        //are we connected to a network already?
        activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI || (wifiInfo = wifiManager.getConnectionInfo()).getSSID().compareTo(WIFI_SSID) != 0)
        {
            List<WifiConfiguration> items = wifiManager.getConfiguredNetworks();
            WifiConfiguration wifindus_public = null;
            int id = -1;

            for (WifiConfiguration item : items)
            {
                if (item.SSID.compareTo(WIFI_SSID) == 0)
                    wifindus_public = item;
            }

            //could not find wfu_public, create it manually,
            if (wifindus_public == null)
            {
                wifindus_public = new WifiConfiguration();
                wifindus_public.SSID = WIFI_SSID;
                wifindus_public.priority = 1000;
                wifindus_public.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wifindus_public.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifindus_public.preSharedKey = WIFI_PSK;

                id = wifiManager.addNetwork(wifindus_public);
            }
            else
                id = wifindus_public.networkId;

            if (id != -1)
            {
                wifiManager.enableNetwork(id, true);
                wifiManager.reconnect();
            }
        }
    }

    @Override
    protected void cleanup()
    {

    }
}
