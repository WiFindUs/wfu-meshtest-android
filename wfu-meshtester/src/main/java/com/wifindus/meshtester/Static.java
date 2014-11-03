package com.wifindus.meshtester;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by marzer on 25/04/2014.
 */
public abstract class Static
{
    public static final Random random = new Random();
    public static final DecimalFormat locationFormat = new DecimalFormat("#.########");

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

    public final static void setMobileDataEnabled(final ConnectivityManager connectivityManager, boolean enabled)
    {
        try
        {
            final Class<?> conmanClass = Class.forName(connectivityManager.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(connectivityManager);
            final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        }
        catch (Exception e)
        {
            Log.e(Static.class.getSimpleName(), e.getClass().getSimpleName() + " thrown during setMobileDataEnabled()");
        }
    }

    public static void launchBluetoothSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        context.startActivity(intent);
    }

    public static final void launchMobileDataSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        context.startActivity(intent);
    }

    public static final void launchWifiSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        context.startActivity(intent);
    }

    public static final void launchNetworkSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        context.startActivity(intent);
    }

    public static final void launchLocationSettings(final Context context)
    {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * Convenience wrapper around the InputStream close() method to deal with IOExceptions.
     * @param stream the stream to close.
     * @return <b>true</b> if the provided stream was closed without exception, <b>false</b> if it was null or an exception was thrown.
     */
    public final static boolean closeStream(final InputStream stream)
    {
        if (stream == null) return false;
        try {
            stream.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Convenience wrapper around the InputStream close() method to deal with IOExceptions.
     * @param reader the stream to close.
     * @return <b>true</b> if the provided stream was closed without exception, <b>false</b> if it was null or an exception was thrown.
     */
    public final static boolean closeStream(final Reader reader)
    {
        if (reader == null) return false;
        try {
            reader.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Convenience wrapper around the OutputStream's close() method to deal with IOExceptions.
     * @param stream the stream to close.
     * @return <b>true</b> if the provided stream was closed without exception, <b>false</b> if it was null or an exception was thrown.
     */
    public final static boolean closeStream(final OutputStream stream)
    {
        if (stream == null) return false;
        try {
            stream.flush();
            stream.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Temporarily Flashes the control with a highlight colour.
     * @param view the control to flash
     */
    public static final void flashControl(View view, final int duration, final int repeats)
    {
        if (view == null)
            return;

        int start = Color.argb(0x00, 0x00, 0xFF, 0xFF);
        int end = Color.argb(0x44, 0xFF, 0xFF, 0xFF);

        //iew.getBackground().get

        android.animation.ValueAnimator va = android.animation.ObjectAnimator.ofInt(view, "backgroundColor", start, end);
        va.setDuration(duration);
        va.setEvaluator(new android.animation.ArgbEvaluator());
        va.setRepeatCount(repeats);
        va.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        va.start();
    }

    public static final String randomString(int lenmin, int lenmax)
    {
        StringBuilder randomStringBuilder = new StringBuilder();
        int length = lenmin + random.nextInt(lenmax - lenmin);
        char tempChar;
        for (int i = 0; i < length; i++){
            tempChar = (char) (97 + random.nextInt(25));
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public static final void deleteRecursive(File fileOrDirectory, boolean deleteBase)
    {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child, true);

        if (deleteBase)
        {
            if (fileOrDirectory.delete())
                Log.i(Static.class.getSimpleName(), "Deleted " + (fileOrDirectory.isDirectory() ? "directory" : "file") + " \"" + fileOrDirectory.getPath()+"\"");
            else
                Log.e(Static.class.getSimpleName(), "Error deleting " + (fileOrDirectory.isDirectory() ? "directory" : "file") + " \"" + fileOrDirectory.getPath()+"\"");
        }
    }

    public static final void deleteRecursive(File fileOrDirectory)
    {
        deleteRecursive(fileOrDirectory, true);
    }

    public static final String formatTimer(long milliseconds)
    {
        String suffix = "";
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
            suffix = "millisecond";
        return "" + milliseconds + " " + suffix + (milliseconds > 1 ? "s" : "");
    }

    public static final void broadcastSimpleIntent(Context context, String action)
    {
        if (context == null || action == null)
            return;
        Intent intent = new Intent();
        intent.setAction(action);
        context.sendBroadcast(intent);
    }
}
