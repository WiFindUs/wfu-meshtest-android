package com.wifindus.meshtester.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wifindus.PingResult;
import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by marzer on 3/11/2014.
 */
public class PingFragment extends BaseFragment
{
    private static final String TAG = PingFragment.class.getName();

    private EditText rangeText = null;
    private TextView pingText = null;
    private ScrollView pingScroll = null;
    private Button startStopButton;

    public PingFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_pinger, container, false);
        pingText = (TextView)view.findViewById(R.id.text);
        pingScroll = (ScrollView)view.findViewById(R.id.scroll);
        startStopButton = (Button)view.findViewById(R.id.start_stop_button);
        startStopButton.setOnClickListener(startStopClickListener);
        rangeText = (EditText)view.findViewById(R.id.field_node_range);
        return view;
    }

    private View.OnClickListener startStopClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view)
        {
            if (view != startStopButton)
                return;

            if (MeshApplication.isPingThreadRunning())
                MeshApplication.stopPingThread(PingFragment.this.getActivity());
            else
                MeshApplication.startPingThread(PingFragment.this.getActivity(), rangeText.getText().toString().trim());
        }
    };

    @Override
    public void update()
    {
        if (MeshApplication.isPingThreadRunning())
        {
            startStopButton.setText(R.string.ping_stop);
            rangeText.setEnabled(false);

            StringBuilder sb = new StringBuilder();
            TreeMap<Integer, PingResult> sorted
                = new TreeMap<Integer, PingResult>(MeshApplication.getNodePings());
            for (Map.Entry<Integer, PingResult> entry : sorted.entrySet())
            {
                sb.append("<b>Node " + entry.getKey() + ":</b> ");
                PingResult result = entry.getValue();
                if (result == PingResult.WAITING)
                    sb.append("<font color=\"gray\">...</font> ");
                else
                {
                    if (result.error)
                        sb.append("<i><font color=\"#CC0000\">error.</font></i> ");
                    else {
                        sb.append(colorHTML("%.2f ms",result.averageTime, 200, 150, 100.0));
                        sb.append(colorHTML(" (%.2f%% loss)",result.loss, 50.0, 30.0, 20.0));
                    }
                }

                sb.append("<br>\n");
            }
            pingText.setText(Html.fromHtml(sb.toString()));
        }
        else
        {
            startStopButton.setText(R.string.ping_start);
            rangeText.setEnabled(true);
            pingText.setText("");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        update();
    }

    private String colorHTML(String format, double value, double red, double orange, double yellow)
    {
        String color = "";
        if (value >= yellow)
        {
            if (value >= red)
                color = "#CC0000";
            else if (value >= orange)
                color="#FF9933";
            else
                color = "#FFCC00";
        }
        String output = String.format(format, value);
        if (!color.isEmpty())
            output = String.format("<font color=\"%s\">%s</font>", color, output);
        return output;
    }

    @Override
    public String logTag(){
        return TAG;
    }
}
