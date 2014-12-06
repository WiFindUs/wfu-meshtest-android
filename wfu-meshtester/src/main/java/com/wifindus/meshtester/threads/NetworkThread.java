package com.wifindus.meshtester.threads;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.wifindus.BaseThread;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.logs.Logger;
import com.wifindus.meshtester.SignalStrengthAnalyzer;
import com.wifindus.meshtester.SignalStrengthData;
import com.wifindus.meshtester.Static;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Created by marzer on 25/04/2014.
 */
public class NetworkThread extends BaseThread
{
    private static final String TAG = NetworkThread.class.getName();
    private volatile boolean hasWifi = false;
    private volatile WifiInfo wifiInfo;
    private volatile WifiManager wifiManager = null;
    private volatile ConnectivityManager connectivityManager = null;
    private static final String WIFI_SSID = "wifindus_public";
    private static final String WIFI_PSK = "a8jFIVcag82H461";
    private static final String WIFI_LOCK_TAG = "robust 17 s3's for 700";
    private volatile WifiManager.WifiLock wifiLock = null;
	private volatile boolean scanResultsAvailable = false;
	private volatile WifiConfiguration wifindus_public = null;

    public static final int SCAN_RUNS = 5;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public NetworkThread(Context launchingContext)
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
	public long timeoutLength()
	{
		long time = MeshApplication.isMeshConnected() ? 30000 : 10000;
		if (!MeshApplication.isBatteryCharging())
		{
			float battery = MeshApplication.getBatteryPercentage();
			if (battery <= 0.5f)
				time = (3*time)/2;
			if (battery <= 0.25f)
				time = (3*time)/2;
		}
		return time;
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
    protected void iteration()
    {
		long count = 0;
		boolean forcedMesh = MeshApplication.getForceMeshConnection();

		if (forcedMesh)
		{
			//ensure wifi is enabled
			int wifiManagerState = wifiManager.getWifiState();
			if (wifiManagerState != WifiManager.WIFI_STATE_ENABLED)
			{
				MeshApplication.updateMeshConnected(logContext(), false);
				MeshApplication.updateMeshAddress(logContext(), -1);
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
						if (isCancelled())
							return;
					}
				}
			}

			//acquire wifi lock
			if (wifiLock == null)
			{
				Logger.i(this, "Aquiring WiFi lock...");
				wifiLock = wifiManager.createWifiLock(WIFI_LOCK_TAG);
				wifiLock.acquire();
			}

			if (isCancelled())
				return;

			//create/identify WFU network configuration
			if (wifindus_public == null)
			{
				Logger.i(this, "Creating WiFindUs network profile...");
				List<WifiConfiguration> items = wifiManager.getConfiguredNetworks();
				boolean isNew = false;
				for (WifiConfiguration item : items)
				{
                    if (compareSSIDs(item.SSID,WIFI_SSID))
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
			}

			if (isCancelled())
				return;

			//get our current network information
			boolean onWiFindUsAP = false;
            boolean initiallyOnWifi = false;
			if (waitForWifiConnection())
			{
				wifiInfo = wifiManager.getConnectionInfo();
				Logger.i(this, "Current Network: " + wifiInfo.getSSID().trim());
				Logger.i(this, "Current AP: " + wifiInfo.getBSSID().trim());
				onWiFindUsAP = compareSSIDs(wifiInfo.getSSID(),WIFI_SSID);
                initiallyOnWifi = true;

                if (onWiFindUsAP)
                {
                    MeshApplication.updateMeshConnected(logContext(), true);
                    MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
                }
			}
            if (onWiFindUsAP)
                Logger.i(this, "Currently on WFU mesh.");

			//scan for wifindus ap's
            if (!MeshApplication.isMeshConnected())
                Logger.i(this, "Scanning local AP's...");
			SignalStrengthAnalyzer analyzer = new SignalStrengthAnalyzer(SCAN_RUNS);
            for (int i = 0; i < SCAN_RUNS; i++)
            {
                scanResultsAvailable = false;
                wifiManager.startScan();
                while (!scanResultsAvailable) //set by broadcast receiver in service
                {
                    safesleep(1000);
					if (isCancelled())
						return;
                }

                //process scan results, looking for WFU network AP's
                List<ScanResult> scanResults = wifiManager.getScanResults();
                if (scanResults == null || scanResults.size() == 0)
                    continue;
                for (ScanResult result : scanResults)
                {
                    if (result.SSID == null || !compareSSIDs(result.SSID,WIFI_SSID))
                        continue;
					analyzer.addSample(result.BSSID,i,result.level);
                }

                if (i < (SCAN_RUNS-1))
                    safesleep(3000);

                if (isCancelled())
                    return;
            }

			//analyze the data
			SignalStrengthData bestAP = analyzer.analyze().getBest();
			Logger.i(this, analyzer.toString());

			//if we're on a wfu ap, compare signal strengths
			if (onWiFindUsAP)
			{
				SignalStrengthData currentAP = analyzer.getByBSSID(wifiInfo.getBSSID());
				if (currentAP == bestAP || currentAP.getMean() <= 70 || currentAP.getTier() >= bestAP.getTier())
				{
					Logger.i(this, "Current AP OK ("+currentAP.getMean()+"dbm).");
					MeshApplication.updateMeshConnected(logContext(), true);
					MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
					return;
				}

				Logger.i(this, "Better AP found ("+bestAP.getMean()+"dbm), migrating...");
			}
			else
			{
				if (bestAP == null)
				{
					Logger.e(this, "No WiFindUs AP's found.");
					MeshApplication.updateMeshConnected(logContext(), false);
					MeshApplication.updateMeshAddress(logContext(), -1);
					return;
				}

				Logger.i(this, "WiFindUs AP found ("+bestAP.getMean()+"dbm), connecting...");

                if (initiallyOnWifi)
				    wifiManager.disconnect();
				safesleep(1000);
				if (isCancelled())
					return;
			}

			//perform the migration
			wifindus_public.BSSID = bestAP.getBSSID();
			wifiManager.updateNetwork(wifindus_public);
			wifiManager.enableNetwork(wifindus_public.networkId, false);
			wifiManager.saveConfiguration();

            if(!onWiFindUsAP)
			{
				wifiManager.reconnect();
				safesleep(1000);
				if (isCancelled())
					return;
			}

			//watch the migration
			if (waitForWifiConnection())
			{
				wifiInfo = wifiManager.getConnectionInfo();
				if (wifiInfo != null && compareSSIDs(wifiInfo.getSSID(),WIFI_SSID))
				{
					if (bestAP.getBSSID().compareTo(wifiInfo.getBSSID()) == 0)
						Logger.i(this, (onWiFindUsAP ? "Migrated" : "Connected") + " OK.");
					else
						Logger.w(this, (onWiFindUsAP ? "Migrated" : "Connected") + " OK, but different AP?");
					MeshApplication.updateMeshAddress(logContext(), wifiInfo.getIpAddress());
					MeshApplication.updateMeshConnected(logContext(), true);
				}
				else
				{
					if (!MeshApplication.isMeshConnected())
						Logger.e(this, (onWiFindUsAP ? "Migrating" : "Connecting") + " to mesh failed!");
					MeshApplication.updateMeshAddress(logContext(), -1);
					MeshApplication.updateMeshConnected(logContext(), false);
				}
			}
			else
			{
				Logger.e(this, "Wifi reconnection failed.");
				MeshApplication.updateMeshConnected(logContext(), false);
				MeshApplication.updateMeshAddress(logContext(), -1);
			}
		}
		else
		{
			NetworkInfo ninfo = connectivityManager.getActiveNetworkInfo();
			boolean connected = ninfo != null && ninfo.isConnectedOrConnecting();
			if (connected != MeshApplication.isMeshConnected())
			{
				if (connected)
					Logger.i(this, "Network connection OK.");
				else
					Logger.e(this, "Network connection lost.");
			}
			MeshApplication.updateMeshConnected(logContext(), connected);
			MeshApplication.updateMeshAddress(logContext(), -1);
		}
    }

    @Override
    protected void cleanup()
    {
        if (wifindus_public != null && wifindus_public.networkId > -1)
            wifiManager.removeNetwork(wifindus_public.networkId);
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
    }

	private boolean waitForConnection(int type)
	{
		NetworkInfo network = type < 0 ? connectivityManager.getActiveNetworkInfo() :
			connectivityManager.getNetworkInfo(type);
		if (networkIsConnectedOrConnecting(network))
		{
			Logger.i(this, "Waiting for connection...");
			while (network != null && !networkIsConnected(network))
			{
				safesleep(1000);
				if (isCancelled() ||
				    !networkIsConnectedOrConnecting(network = type < 0
                        ? connectivityManager.getActiveNetworkInfo() : connectivityManager.getNetworkInfo(type)))
					break;
			}
		}
		return networkIsConnected(network);
	}

    private boolean networkIsConnectedOrConnecting(NetworkInfo ninfo)
    {
        if (ninfo == null)
            return false;
        if (ninfo.isConnectedOrConnecting())
            return true;
        NetworkInfo.State state = ninfo.getState();
        return state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING;
    }

    private boolean networkIsConnected(NetworkInfo ninfo)
    {
        if (ninfo == null)
            return false;
        if (ninfo.isConnected())
            return true;
        NetworkInfo.State state = ninfo.getState();
        return state == NetworkInfo.State.CONNECTED;
    }

	private boolean waitForWifiConnection()
	{
		return waitForConnection(ConnectivityManager.TYPE_WIFI);
	}

    private String formatSSID(String ssid)
    {
        if (ssid == null || (ssid = ssid.trim()).length() == 0)
            return "\"\"";
        return (ssid.substring(0, 1).compareTo("\"") == 0 ? "" : "\"")
                + ssid + (ssid.substring(ssid.length()-1).compareTo("\"") == 0 ? "" : "\"");
    }

    private boolean compareSSIDs(String ssidA, String ssidB)
    {
        return formatSSID(ssidA).compareTo(formatSSID(ssidB)) == 0;
    }
}
