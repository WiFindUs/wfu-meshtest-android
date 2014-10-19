package com.wifindus.meshtester;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

import com.wifindus.meshtester.interfaces.MeshApplicationSubscriber;
import com.wifindus.meshtester.meshservicethreads.LocationThread;
import com.wifindus.meshtester.meshservicethreads.UpdateThread;
import com.wifindus.meshtester.meshservicethreads.WifiThread;

public class MeshService extends Service implements MeshApplicationSubscriber
{
    private boolean ready = false;
    public static final String RESTORE_FROM_SERVICE = "RESTORE_FROM_SERVICE";
    private WifiThread wifiThread = null;
    private LocationThread locationThread = null;
    private UpdateThread updateThread = null;

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
            .setLights(Color.RED, 1000, 3000)
            .setSubText(getString(R.string.app_notification_subtext));
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        //set foreground
        startForeground(1337, notification);

        //attach to application
        app().setMeshService(this);

        //start threads
        wifiThread = new WifiThread();
        wifiThread.start();
        locationThread = new LocationThread();
        locationThread.start();
        updateThread = new UpdateThread();
        updateThread.start();

        //set ready
        ready = true;

        //return sticky flag
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        updateThread.cancelThread();
        locationThread.cancelThread();
        wifiThread.cancelThread();
        app().setMeshService(null);
        stopForeground(true);
    }

    @Override
    public MeshApplication app()
    {
        return (MeshApplication)getApplication();
    }

    public boolean isReady()
    {
        return ready;
    }
}
