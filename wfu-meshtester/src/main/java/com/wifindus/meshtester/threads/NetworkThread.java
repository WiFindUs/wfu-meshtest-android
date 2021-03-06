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
import com.wifindus.Static;

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
		/*long time = MeshApplication.isMeshConnected() ? 30000 : 10000;
		if (!MeshApplication.isBatteryCharging())
		{
			float battery = MeshApplication.getBatteryPercentage();
			if (battery <= 0.5f)
				time = (3*time)/2;
			if (battery <= 0.25f)
				time = (3*time)/2;
		}
		return time;
		*/
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

		if (Static.isAirplaneModeOn(this.logContext()))
			Logger.w(this, "Airplane mode is on; network functions may not work!");

		Logger.i(this, "WiFi thread OK.");

		//create/identify WFU network configuration
		if (wifindus_public == null)
			wifindus_public = getWifindusConfiguration();

		//acquire wifi lock
		if (wifiLock == null)
		{
			Logger.i(this, "Aquiring WiFi lock...");
			wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG);
			wifiLock.acquire();
		}
    }

    @Override
    protected void iteration()
	{
		//if (MeshApplication.getForceMeshConnection())
		//	performForcedMeshIteration();
		//else
			performUnforcedIteration();
		safesleep(1000);
		updateMeshNode(null);
	}

	private void performForcedMeshIteration()
	{
		//ensure wifi is enabled
		int wifiManagerState = wifiManager.getWifiState();
		if (wifiManagerState != WifiManager.WIFI_STATE_ENABLED)
		{
			MeshApplication.updateMeshConnected(this, false);
			if (wifiManagerState != WifiManager.WIFI_STATE_ENABLING)
			{
				wifiManager.setWifiEnabled(true);
				Logger.w(this, "WiFi disabled, re-enabling...");
				int count = 0;
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

		if (isCancelled())
			return;

		//create/identify WFU network configuration
		if (wifindus_public == null)
			wifindus_public = getWifindusConfiguration();

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
			onWiFindUsAP = compareSSIDs(wifiInfo.getSSID(), WIFI_SSID);
			initiallyOnWifi = true;

			if (onWiFindUsAP)
				MeshApplication.updateMeshConnected(this, true);
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
				if (result.SSID == null || !compareSSIDs(result.SSID, WIFI_SSID))
					continue;
				analyzer.addSample(result.BSSID, i, result.level);
			}

			if (i < (SCAN_RUNS - 1))
				safesleep(3000);

			if (isCancelled())
				return;
		}
		for (Map.Entry<String, SignalStrengthData> entry : analyzer.getStrengths().entrySet())
			MeshApplication.addSignalStrengthHistory(entry.getValue());

		//analyze the data
		SignalStrengthData bestAP = analyzer.analyze().getBest();
		Logger.i(this, analyzer.toString());

		//if we're on a wfu ap, compare signal strengths
		if (onWiFindUsAP)
		{
			SignalStrengthData currentAP = analyzer.getByBSSID(wifiInfo.getBSSID());
			if (currentAP == bestAP || currentAP.getMean() <= 70 || currentAP.getTier() >= bestAP.getTier())
			{
				Logger.i(this, "Current AP OK (" + currentAP.getMean() + "dbm).");
				MeshApplication.updateMeshConnected(this, true);
				return;
			}

			Logger.i(this, "Better AP found (" + bestAP.getMean() + "dbm), migrating...");
		}
		else
		{
			if (bestAP == null)
			{
				Logger.e(this, "No WiFindUs AP's found.");
				MeshApplication.updateMeshConnected(this, false);
				return;
			}

			Logger.i(this, "WiFindUs AP found (" + bestAP.getMean() + "dbm), connecting...");

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

		if (!onWiFindUsAP)
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
			if (wifiInfo != null && compareSSIDs(wifiInfo.getSSID(), WIFI_SSID))
			{
				if (bestAP.getBSSID().compareTo(wifiInfo.getBSSID()) == 0)
					Logger.i(this, (onWiFindUsAP ? "Migrated" : "Connected") + " OK.");
				else
					Logger.w(this, (onWiFindUsAP ? "Migrated" : "Connected") + " OK, but different AP?");
				MeshApplication.updateMeshConnected(this, true);
			}
			else
			{
				if (!MeshApplication.isMeshConnected())
					Logger.e(this, (onWiFindUsAP ? "Migrating" : "Connecting") + " to mesh failed!");
				MeshApplication.updateMeshConnected(this, false);
			}
		}
		else
		{
			Logger.e(this, "Wifi reconnection failed.");
			MeshApplication.updateMeshConnected(this, false);
		}
	}

	private void performUnforcedIteration()
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
		MeshApplication.updateMeshConnected(this, connected);
    }

    @Override
    protected void cleanup()
    {
        if (wifiLock != null && wifiLock.isHeld())
            wifiLock.release();
    }

	/////////////////////////////////////////////////////////////////////
	// PROTECTED METHODS
	/////////////////////////////////////////////////////////////////////

	private void updateMeshNode(String ipv4address)
	{
		if (ipv4address == null)
			ipv4address = Static.getIPAddress(true);

		Matcher matcher = null;
		if (ipv4address == null
			|| (ipv4address = ipv4address.trim()).length() == 0
			|| !(matcher = Static.PATTERN_IPV4_ADDRESS.matcher(ipv4address)).matches())
		{
			MeshApplication.updateMeshNode(this, 0);
			return;
		}

		if (matcher.group(1).equals("172") && matcher.group(2).equals("16"))
			MeshApplication.updateMeshNode(this, Integer.parseInt(matcher.group(3)));
		else
			MeshApplication.updateMeshNode(this, 0);
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

	private WifiConfiguration getWifindusConfiguration()
	{
		//create/identify WFU network configuration
		WifiConfiguration config = null;

		Logger.i(this, "Searching for WiFi profile...");
		List<WifiConfiguration> items = wifiManager.getConfiguredNetworks();
		boolean isNew = false;
		if (items != null && items.size() > 0)
		{
			for (WifiConfiguration item : items)
			{
				if (item == null)
					continue;
				if (compareSSIDs(item.SSID, WIFI_SSID))
				{
					config = item;
					break;
				}
			}
		}
		if (config == null)
		{
			config = new WifiConfiguration();
			isNew = true;
			Logger.w(this, "WiFi profile not found! Creating...");
			config.SSID = "\"" + WIFI_SSID + "\"";
			config.preSharedKey = "\"" + WIFI_PSK + "\"";
			config.priority = 99999;
			config.status = WifiConfiguration.Status.ENABLED;
		}
		else
			Logger.i(this, "WiFi profile found OK.");
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
		config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
		if (isNew)
		{
			wifiManager.addNetwork(config);
			Logger.i(this, "WiFi profile created OK.");
		}
		else
			wifiManager.updateNetwork(config);
		wifiManager.saveConfiguration();

		return config;
	}
}
