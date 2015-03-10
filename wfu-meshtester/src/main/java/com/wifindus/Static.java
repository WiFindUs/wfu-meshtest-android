package com.wifindus;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marzer on 25/04/2014.
 */
public abstract class Static
{
	public static final double EARTH_RADIUS_MEAN = 6378.1370f;
	public static final double DEGREES_TO_RADIANS = 0.0174532925;
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
		+"\\s*", Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_TRAILING_ZEROES
		= Pattern.compile("\\.00+", Pattern.CASE_INSENSITIVE);
	public static final Pattern PATTERN_FLOAT_ZERO
		= Pattern.compile("0.0+", Pattern.CASE_INSENSITIVE);

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
		double latitudeDistance = (latA - latB) * DEGREES_TO_RADIANS;
		double longitudeDistance = (longA - longB) * DEGREES_TO_RADIANS;
		double d = Math.sin(latitudeDistance/2.0) * Math.sin(latitudeDistance/2.0) +
			Math.cos(latB * DEGREES_TO_RADIANS) * Math.cos(latA * DEGREES_TO_RADIANS) *
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
}
