package com.wifindus.meshtester.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wifindus.logs.Logger;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;
import com.wifindus.meshtester.Static;

import java.util.regex.Matcher;


public class StatusFragment extends BaseFragment
{
    private TextView connectionState, connectedSince, meshAddress,
            node, nodeAddress, id, uptime, location, lastCleaned, battery, server;
	private Button changeServerButton;
    private Handler timerHandler = new Handler();
    private static final String TAG = StatusFragment.class.getName();

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
		id = (TextView)view.findViewById(R.id.field_hash);
        uptime = (TextView)view.findViewById(R.id.field_uptime);
        location = (TextView)view.findViewById(R.id.field_location);
        lastCleaned = (TextView)view.findViewById(R.id.field_mesh_last_cleaned);
		battery = (TextView)view.findViewById(R.id.field_battery);
		changeServerButton = (Button)view.findViewById(R.id.server_change_button);
		changeServerButton.setOnClickListener(changeServerClickListener);
		server = (TextView)view.findViewById(R.id.field_mesh_server);
		server.setText(getResources().getString(R.string.data_host_port,MeshApplication.getServerHostName(),MeshApplication.getServerPort()));
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        update();
        timerHandler.postDelayed(timerRunnable, 250);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void update()
    {
		id.setText(Long.toHexString(MeshApplication.getID()).toUpperCase());
        if (MeshApplication.isMeshConnected())
        {
            connectionState.setText("Yes");
            meshAddress.setText(MeshApplication.getMeshHostName());

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

		battery.setText(getResources().getString(R.string.data_battery,
			(int)(MeshApplication.getBatteryPercentage()*100.0f),
				MeshApplication.isBatteryCharging() ? "(charging)" : ""));

        updateUptime();
    }

    @Override
    public String logTag(){
        return TAG;
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
            timerHandler.postDelayed(this, 250);
        }
    };

	private View.OnClickListener changeServerClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view)
		{
			if (view != changeServerButton)
				return;

			//create text edit box
			final EditText text = new EditText(StatusFragment.this.getActivity());
			text.setRawInputType(InputType.TYPE_CLASS_TEXT);
			text.setFilters(new InputFilter[] {new InputFilter.LengthFilter(22)});
			text.setText(getResources().getString(R.string.data_host_port,MeshApplication.getServerHostName(),MeshApplication.getServerPort()));

			//build the dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(StatusFragment.this.getActivity());
			builder.setTitle(R.string.status_change_server)
				.setMessage(R.string.status_change_server_help_text)
				.setView(text)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface di, int i)
					{
						//parse using regex
						Matcher match = Static.PATTERN_HOSTNAME_PORT.matcher(text.getText().toString().trim());
						if (!match.find())
						{
							Toast.makeText(StatusFragment.this.getActivity(),
								getResources().getString(R.string.status_change_server_invalid_format),
								Toast.LENGTH_LONG).show();
							return;
						}

						//get hostname
						String hostname = match.group(1);

						//get port
						int port = -1;
						if (match.groupCount() >= 3)
						{
							try
							{
								port = Integer.parseInt(match.group(2));
							} catch (NumberFormatException ex) { }
						}
						if (port < 0)
							port = 33339;
						else if (port < 1024 || port > 65535)
						{
							Toast.makeText(StatusFragment.this.getActivity(),
								getResources().getString(R.string.status_change_server_invalid_port),
								Toast.LENGTH_LONG).show();
							return;
						}

						//update app
						if (MeshApplication.setServer(StatusFragment.this.getActivity(), hostname, port))
						{
							server.setText(getResources().getString(R.string.data_host_port, MeshApplication.getServerHostName(), MeshApplication.getServerPort()));
							Toast.makeText(StatusFragment.this.getActivity(),
								getResources().getString(R.string.status_change_server_ok),
								Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
		}
	};
}
