package com.wifindus.meshtester;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import com.wifindus.meshtester.logs.LogSender;
import com.wifindus.meshtester.logs.Logger;
import com.wifindus.meshtester.threads.LocationThread;
import com.wifindus.meshtester.threads.UpdateThread;
import com.wifindus.meshtester.threads.WifiThread;

public class MeshService extends Service implements LogSender
{
    public static final String RESTORE_FROM_SERVICE = "RESTORE_FROM_SERVICE";
    private static final String TAG = UpdateThread.class.getName();
    private volatile WifiThread wifiThread = null;
    private volatile LocationThread locationThread = null;
    private volatile UpdateThread updateThread = null;
    private volatile boolean ready = false;

    public MeshService()
    {

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //pending intent - activity to launch when we click the notification
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        appIntent.putExtra(RESTORE_FROM_SERVICE,true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

        //build notification (according to API level)
        Notification.Builder builder = new Notification.Builder(this)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setLights(Color.MAGENTA, 200, 3000);
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setSubText(getString(R.string.app_notification_subtext));
            notification = builder.build();
        }
        else
            notification = builder.getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        //set foreground
        startForeground(1337, notification);

        //attach to application
        MeshApplication.setMeshService(this);

        //start threads
        wifiThread = new WifiThread(this);
        wifiThread.start();
        locationThread = new LocationThread(this);
        locationThread.start();
        updateThread = new UpdateThread(this);
        updateThread.start();

        //set ready
        ready = true;
        Logger.i(this, "Service started OK.");

        //return sticky flag
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        updateThread.cancelThread();
        locationThread.cancelThread();
        wifiThread.cancelThread();
        MeshApplication.setMeshService(null);
        Logger.i(this, "Service stopped.");
        stopForeground(true);
    }

    public boolean isReady()
    {
        return ready;
    }

    @Override
    public String logTag()
    {
        return TAG;
    }

    @Override
    public Context logContext()
    {
        return this;
    }
}
