package com.wifindus;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by marzer on 25/04/2014.
 */
public abstract class Static
{
	public static final double EARTH_RADIUS_MEAN = 6378.1370f;
	public static final double DEGREES_TO_RADIANS = 0.0174532925;
	public static final Pattern PATTERN_PATH_SEPARATOR = Pattern.compile(File.separator);
	public static final Pattern PATTERN_HOSTNAME_PORT = Pattern.compile("\\s*"
		//hostname/ip
		+"("
		//ipv4 address
		+"(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]"
		+"(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]"
		+"(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]"
		+"(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))"
		//hostname
		+"|(?:(?:(?:[A-Z0-9]|[A-Z0-9][A-Z0-9\\-]*[A-Z0-9])[.])*(?:[A-Z0-9]|[A-Z0-9][A-Z0-9\\-]*[A-Z0-9]))"
		//end hostname/ip
		+")"
		//port (optional)
		+ "([:][0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])?"
		+"\\s*", Pattern.CASE_INSENSITIVE
	);
	public static final Pattern PATTERN_IPV4_ADDRESS = Pattern.compile("\\s*" +
		"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]" +
		"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]" +
		"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[.]" +
		"(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
		+"\\s*", Pattern.CASE_INSENSITIVE
	);
    /**
     * Detect if the system's Airplane mode is turned on.
     * @param context application context to use
     * @return <b>true</b> if Airplane mode is turned on.
     */
    public final static boolean isAirplaneModeOn(final Context context)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        }
        else
        {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    public static final void launchWifiSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        context.startActivity(intent);
    }

    public static final void launchLocationSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public static final String formatTimer(long milliseconds, boolean includeMilliseconds)
    {
        String suffix = "";
		boolean isms = false;
        if (milliseconds > 86400000) //days
        {
            milliseconds /= 86400000;
            suffix = "day";
        }
        else if (milliseconds > 3600000) //hours
        {
            milliseconds /= 3600000;
            suffix = "hour";
        }
        else if (milliseconds > 60000) //minutes
        {
            milliseconds /= 60000;
            suffix = "minute";
        }
        else if (milliseconds > 1000) //seconds
        {
            milliseconds /= 1000;
            suffix = "second";
        }
        else
		{
			suffix = "millisecond";
			isms = true;
		}
        return (!includeMilliseconds && isms ? "less than a second" :
			"" + milliseconds + " " + suffix + (milliseconds > 1 ? "s" : "")
			 );
    }

	public static final String formatTimer(long milliseconds)
	{
		return formatTimer(milliseconds, false);
	}

    public static final void broadcastSimpleIntent(Context context, String action)
    {
        if (context == null || action == null)
            return;
        Intent intent = new Intent();
        intent.setAction(action);
        context.sendBroadcast(intent);
    }

    public static final String ping(String address, int count) throws IOException
    {
        if (count < 0 || count > 50)
			count = 10;

		Process process = Runtime.getRuntime().exec(
                "/system/bin/ping -c "+count+" -i 0.2 -n -q " + address);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream()));
        int i;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((i = reader.read(buffer)) > 0)
            output.append(buffer, 0, i);
        reader.close();

        return output.toString();
    }

	public static double distanceTo(double latA, double longA, double latB, double longB)
	{
		double latitudeDistance = Math.toRadians(latA - latB);
		double longitudeDistance = Math.toRadians(longA - longB);
		double d = Math.sin(latitudeDistance/2.0) * Math.sin(latitudeDistance/2.0) +
			Math.cos(Math.toRadians(latB)) * Math.cos(Math.toRadians(latA)) *
				Math.sin(longitudeDistance/2.0) * Math.sin(longitudeDistance/2.0);

		return (2.0 * Math.atan2(Math.sqrt(d), Math.sqrt(1.0-d))) * EARTH_RADIUS_MEAN * 1000.0;
	}

	public static boolean isExternalStorageWritable()
	{
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public static boolean isExternalStorageReadable()
	{
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) ||
			Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	public static String[] getStorageDirectories()
	{
		// Final set of paths
		final Set<String> rv = new HashSet<String>();
		// Primary physical SD-CARD (not emulated)
		final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
		// All Secondary SD-CARDs (all exclude primary) separated by ":"
		final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
		// Primary emulated SD-CARD
		final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
		if(TextUtils.isEmpty(rawEmulatedStorageTarget))
		{
			// Device has physical external storage; use plain paths.
			if(TextUtils.isEmpty(rawExternalStorage))
			{
				// EXTERNAL_STORAGE undefined; falling back to default.
				rv.add("/storage/sdcard0");
			}
			else
			{
				rv.add(rawExternalStorage);
			}
		}
		else
		{
			// Device has emulated storage; external storage paths should have
			// userId burned into them.
			final String rawUserId;
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
			{
				rawUserId = "";
			}
			else
			{
				final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
				final String[] folders = PATTERN_PATH_SEPARATOR.split(path);
				final String lastFolder = folders[folders.length - 1];
				boolean isDigit = false;
				try
				{
					Integer.valueOf(lastFolder);
					isDigit = true;
				}
				catch(NumberFormatException ignored)
				{
				}
				rawUserId = isDigit ? lastFolder : "";
			}
			// /storage/emulated/0[1,2,...]
			if(TextUtils.isEmpty(rawUserId))
			{
				rv.add(rawEmulatedStorageTarget);
			}
			else
			{
				rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
			}
		}
		// Add all secondary storages
		if(!TextUtils.isEmpty(rawSecondaryStoragesStr))
		{
			// All Secondary SD-CARDs splited into array
			final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
			Collections.addAll(rv, rawSecondaryStorages);
		}
		return rv.toArray(new String[rv.size()]);
	}

	//credit: http://stackoverflow.com/a/13007325
	public static String getIPAddress(boolean useIPv4)
	{
		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces)
			{
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs)
				{
					if (!addr.isLoopbackAddress())
					{
						String sAddr = addr.getHostAddress().toUpperCase();
						boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						if (useIPv4)
						{
							if (isIPv4)
								return sAddr;
						}
						else
						{
							if (!isIPv4)
							{
								int delim = sAddr.indexOf('%'); // drop ip6 port suffix
								return delim < 0 ? sAddr : sAddr.substring(0, delim);
							}
						}
					}
				}
			}
		}
		catch (Exception ex) { } // eat exceptions
		return "";
	}
}
