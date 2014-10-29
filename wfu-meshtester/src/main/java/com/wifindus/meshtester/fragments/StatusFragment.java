package com.wifindus.meshtester.fragments;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;
import com.wifindus.meshtester.Static;


public class StatusFragment extends BaseFragment
{
    private TextView connectionState, connectedSince, meshAddress,
            node, nodeAddress, hash, uptime, location, lastCleaned;
    private Handler timerHandler = new Handler();

    public StatusFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_status, container, false);
        connectionState = (TextView)view.findViewById(R.id.field_mesh_state);
        connectedSince = (TextView)view.findViewById(R.id.field_mesh_uptime);
        meshAddress = (TextView)view.findViewById(R.id.field_mesh_ip_address);
        node = (TextView)view.findViewById(R.id.field_mesh_node);
        nodeAddress = (TextView)view.findViewById(R.id.field_mesh_node_ip_address);
        hash = (TextView)view.findViewById(R.id.field_hash);
        uptime = (TextView)view.findViewById(R.id.field_uptime);
        location = (TextView)view.findViewById(R.id.field_location);
        lastCleaned = (TextView)view.findViewById(R.id.field_mesh_last_cleaned);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateStatusItems();
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    public void updateStatusItems()
    {
        hash.setText(MeshApplication.getHash());
        if (MeshApplication.isMeshConnected())
        {
            connectionState.setText("Yes");
            meshAddress.setText(MeshApplication.getMeshAddress() == null ?
                "..." : MeshApplication.getMeshAddress().getHostName());

            //todo
            node.setText("");
            nodeAddress.setText("");
        }
        else
        {
            connectionState.setText("No");
            meshAddress.setText("");

            //todo
            node.setText("");
            nodeAddress.setText("");
        }


        Location loc = MeshApplication.getLocation();
        location.setText(loc != null ? getResources().getString(R.string.data_latlong,
                loc.getLatitude(),loc.getLongitude()) : "");

        updateUptime();
    }

    private void updateUptime()
    {
        if (MeshApplication.isMeshConnected())
        {
            long time = System.currentTimeMillis();
            connectedSince.setText(
                Static.formatTimer(time - MeshApplication.getMeshConnectedSince()) + " ago");
            lastCleaned.setText(MeshApplication.lastCleaned() == 0 ? "" :
                Static.formatTimer(time - MeshApplication.lastCleaned()) + " ago");
        }
        else
        {
            connectedSince.setText("");
            lastCleaned.setText("");
        }

        uptime.setText(Static.formatTimer(SystemClock.uptimeMillis()) + " ago");

    }

    private Runnable timerRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            updateUptime();
            timerHandler.postDelayed(this, 1000);
        }
    };
}
