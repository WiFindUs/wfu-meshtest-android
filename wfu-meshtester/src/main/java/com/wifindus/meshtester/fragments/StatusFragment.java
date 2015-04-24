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
import com.wifindus.meshtester.MeshServer;
import com.wifindus.meshtester.R;
import com.wifindus.Static;

import java.util.regex.Matcher;


public class StatusFragment extends BaseFragment
{
    private TextView connectionState, connectedSince,
		 id, uptime, location, battery, serverPrimary, serverSecondary,
            locationTime;
	private CheckBox serverPrimaryEnabled, serverSecondaryEnabled;
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

		MeshServer primary = MeshApplication.getServer(MeshApplication.SERVER_PRIMARY);
		serverPrimary = (TextView)view.findViewById(R.id.field_server_primary);
		serverPrimary.setText(primary.getHostName());
		serverPrimary.setClickable(true);
		serverPrimary.setOnClickListener(changeServerClickListener);
		serverPrimary.setTag(primary);
		serverPrimaryEnabled = (CheckBox)view.findViewById(R.id.network_server_primary_enabled);
		serverPrimaryEnabled.setTag(primary);
		serverPrimaryEnabled.setChecked(primary.isEnabled());
		serverPrimaryEnabled.setOnClickListener(serverEnabledClickListener);

		MeshServer secondary = MeshApplication.getServer(MeshApplication.SERVER_SECONDARY);
		serverSecondary = (TextView)view.findViewById(R.id.field_server_secondary);
		serverSecondary.setText(secondary.getHostName());
		serverSecondary.setClickable(true);
		serverSecondary.setOnClickListener(changeServerClickListener);
		serverSecondary.setTag(secondary);
		serverSecondaryEnabled = (CheckBox)view.findViewById(R.id.network_server_secondary_enabled);
		serverSecondaryEnabled.setTag(secondary);
		serverSecondaryEnabled.setChecked(secondary.isEnabled());
		serverSecondaryEnabled.setOnClickListener(serverEnabledClickListener);

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
		int nodeNum  = MeshApplication.getMeshNode();
		id.setText(MeshApplication.getID().hex());
        connectionState.setText(MeshApplication.isMeshConnected()
			? "Yes"+(nodeNum == 0 ? "" : " (Node " + Integer.toString(nodeNum)) + ")": "No");
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

	private View.OnClickListener serverEnabledClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			CheckBox cb = (CheckBox)view;
			MeshServer server = (MeshServer)cb.getTag();
			server.setEnabled(cb.isChecked());
		}
	};

	private View.OnClickListener changeServerClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view)
		{
			final MeshServer server = (MeshServer)view.getTag();
			final TextView serverText = (TextView)view;

			//create text edit box
			final EditText text = new EditText(StatusFragment.this.getActivity());
			text.setRawInputType(InputType.TYPE_CLASS_TEXT);
			text.setFilters(new InputFilter[] {new InputFilter.LengthFilter(64)});
			text.setText(server.getHostName());

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

						//update app
						if (server.setHostName(match.group(1)))
						{
							serverText.setText(server.getHostName());
							Toast.makeText(StatusFragment.this.getActivity(),
								getResources().getString(R.string.status_change_server_ok),
								Toast.LENGTH_LONG).show();
						}
						else
						{
							Toast.makeText(StatusFragment.this.getActivity(),
								getResources().getString(R.string.status_change_server_error),
								Toast.LENGTH_LONG).show();
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
		}
	};
}
