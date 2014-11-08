package com.wifindus.meshtester.threads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.logs.Logger;

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
    private static final String WIFI_SSID = "wifindus_public";
    private static final String WIFI_PSK = "a8jFIVcag82H461";
    //private static final String WIFI_SSID = "\"Marzer WLAN\"";
   // private static final String WIFI_PSK = "\"omgwtflol87\"";
    private static final String WIFI_LOCK_TAG = "robust 17 s3's for 700";
    private volatile WifiManager.WifiLock wifiLock = null;
	private volatile boolean scanResultsAvailable = false;
	private volatile WifiConfiguration wifindus_public = null;

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

	public void newScanResultsAvailable()
	{
		scanResultsAvailable = true;
	}

    /////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    /////////////////////////////////////////////////////////////////////

    @Override
    protected long iterationInterval()
    {
        return 60000;
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

		//aquire wifi lock
		Logger.i(this, "Aquiring WiFi lock...");
        wifiLock = wifiManager.createWifiLock(WIFI_LOCK_TAG);
        wifiLock.acquire();

		//create/identify WFU network configuration
		Logger.i(this, "Creating WiFindUs network profile...");
		List<WifiConfiguration> items = wifiManager.getConfiguredNetworks();
		boolean isNew = false;
		for (WifiConfiguration item : items)
		{
			if (item.SSID.compareTo("\"" + WIFI_SSID + "\"") == 0)
			{
				wifindus_public = item;
				break;
			}
		}
		if (wifindus_public == null)
		{
			wifindus_public = new WifiConfiguration();
			isNew = true;
		}
		wifindus_public.SSID = "\"" + WIFI_SSID + "\"";
		wifindus_public.preSharedKey = "\"" + WIFI_PSK + "\"";
		wifindus_public.status = WifiConfiguration.Status.ENABLED;
		wifindus_public.hiddenSSID = true;
		wifindus_public.priority = 99999;
		wifindus_public.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		wifindus_public.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		wifindus_public.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		wifindus_public.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		wifindus_public.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		if (isNew)
			wifiManager.addNetwork(wifindus_public);
		else
			wifiManager.updateNetwork(wifindus_public);
		wifiManager.saveConfiguration();

		Logger.i(this, "WiFi thread OK.");
    }

    @Override
    protected void iteration()
    {
        long count = 0;

        //ensure wifi is enabled
        int wifiManagerState = wifiManager.getWifiState();
        if (wifiManagerState != WifiManager.WIFI_STATE_ENABLED)
        {
            MeshApplication.updateMeshConnected(logContext(), false);
            if (wifiManagerState != WifiManager.WIFI_STATE_ENABLING)
            {
                wifiManager.setWifiEnabled(true);
				Logger.w(this, "WiFi disabled, re-enabling...");
				count = 0;
				while (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED)
				{
					safesleep(1000);
					if (++count >= 15)
					{
						Logger.e(this, "Error re-enabling WiFi.");
						return;
					}
				}
            }

        }

		if (isCancelled())
			return;

		//scan for new ap's
		Logger.i(this, "Scanning local AP's...");
		scanResultsAvailable = false;
		wifiManager.startScan();
		count = 0;
		while (!scanResultsAvailable) //set by broadcast receiver in service
		{
			safesleep(1000);
			if (++count >= 15)
			{
				Logger.e(this, "AP scan timed out.");
				return;
			}
		}

		if (isCancelled())
			return;

		//process scan results, looking for WFU network AP's
		List<ScanResult> scanResults = wifiManager.getScanResults();
		if (scanResults == null || scanResults.size() == 0)
		{
			Logger.e(this, "No AP's found.");
			return;
		}
		ScanResult bestFit = null;
		for(ScanResult result : scanResults)
		{
			if (result.SSID == null || result.SSID.compareTo(WIFI_SSID) != 0)
				continue;
			if (bestFit == null || result.level > bestFit.level)
				bestFit = result;
		}
		if (bestFit == null)
		{
			Logger.e(this, "No WiFindUs AP's found.");
			return;
		}

		if (isCancelled())
			return;

		//check current connection
		activeNetwork = connectivityManager.getActiveNetworkInfo();
		if (activeNetwork == null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI)
		{
			Logger.i(this, "AP found, connecting...");
			MeshApplication.updateMeshConnected(logContext(), false);
		}
		else
		{
			wifiInfo = wifiManager.getConnectionInfo();

			if (wifiInfo.getSSID().compareTo("\"" + WIFI_SSID + "\"") == 0
				&& wifiInfo.getBSSID().compareTo(bestFit.BSSID) == 0)
			{
				MeshApplication.updateMeshConnected(logContext(), true);
				MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
				Logger.i(this, "Already connected to best AP.");
				return;
			}

			Logger.i(this, "Better AP found, migrating...");
			wifiManager.disconnect();
			MeshApplication.updateMeshConnected(logContext(), false);
		}

		if (isCancelled())
			return;

		//migrate to new AP
		wifindus_public.BSSID = bestFit.BSSID;
		wifiManager.updateNetwork(wifindus_public);
		wifiManager.enableNetwork(wifindus_public.networkId, false);
		wifiManager.saveConfiguration();
		wifiManager.reconnect();

		//wait and confirm
		count = 0;
		wifiInfo = wifiManager.getConnectionInfo();
		while (wifiInfo == null
			|| wifiInfo.getSSID().compareTo("\"" + WIFI_SSID + "\"") != 0
			|| wifiInfo.getBSSID().compareTo(bestFit.BSSID) != 0)
		{
			safesleep(1000);
			if (++count >= 30)
			{
				Logger.w(this, "Migration check timed out.");
				MeshApplication.updateMeshConnected(logContext(), false);
				return;
			}
		}
		Logger.i(this, "Connected to mesh OK.");
		MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
		MeshApplication.updateMeshConnected(logContext(), true);
    }

    @Override
    protected void cleanup()
    {
        if (wifindus_public != null && wifindus_public.networkId > -1)
            wifiManager.removeNetwork(wifindus_public.networkId);
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
    }
}
