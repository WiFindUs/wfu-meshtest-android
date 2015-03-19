package com.wifindus.meshtester.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;
import com.wifindus.Static;

import java.util.regex.Matcher;


public class StatusFragment extends BaseFragment
{
    private TextView connectionState, connectedSince,
		 id, uptime, location, battery, server,
            locationTime;
	private CheckBox forceMeshNetwork;
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
		id = (TextView)view.findViewById(R.id.field_hash);
        uptime = (TextView)view.findViewById(R.id.field_uptime);
        location = (TextView)view.findViewById(R.id.field_location);
		battery = (TextView)view.findViewById(R.id.field_battery);
        locationTime = (TextView)view.findViewById(R.id.field_device_last_location);
		server = (TextView)view.findViewById(R.id.field_mesh_server);
		server.setText(getResources().getString(R.string.data_host_port,MeshApplication.getServerHostName(),MeshApplication.getServerPort()));
		server.setClickable(true);
		server.setOnClickListener(changeServerClickListener);

		forceMeshNetwork = (CheckBox)view.findViewById(R.id.network_change_force_mesh);
		forceMeshNetwork.setChecked(MeshApplication.getForceMeshConnection());
		forceMeshNetwork.setOnClickListener(forceMeshNetworkClickListener);

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
		id.setText(MeshApplication.getID().hex());
        connectionState.setText(MeshApplication.isMeshConnected() ? "Yes" : "No");

		Double latitude = MeshApplication.getLatitude();
		Double longitude = MeshApplication.getLongitude();
        location.setText(latitude != null && longitude != null
			? getResources().getString(R.string.data_latlong, latitude,longitude) : "");

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
        long time = SystemClock.elapsedRealtime();
		connectedSince.setText(MeshApplication.isMeshConnected() ?
			Static.formatTimer(time - MeshApplication.getMeshConnectedSince()) + " ago"
				: "");
        locationTime.setText(MeshApplication.getLocationTime() == 0 ? "" :
                Static.formatTimer(time - MeshApplication.getLocationTime()) + " ago");
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

	private View.OnClickListener forceMeshNetworkClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			if (view != forceMeshNetwork)
				return;
			MeshApplication.setForceMeshConnection(forceMeshNetwork.isChecked());
		}
	};

	private View.OnClickListener changeServerClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view)
		{
			if (view != server)
				return;

			//create text edit box
			final EditText text = new EditText(StatusFragment.this.getActivity());
			text.setRawInputType(InputType.TYPE_CLASS_TEXT);
			text.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
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
