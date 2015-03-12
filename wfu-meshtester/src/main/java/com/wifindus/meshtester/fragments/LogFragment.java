package com.wifindus.meshtester.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.R;
import com.wifindus.logs.Logger;
import com.wifindus.logs.LoggerItem;

import java.util.List;
import java.util.regex.Pattern;


public class LogFragment extends BaseFragment
{
    private TextView logText = null;
    private ScrollView logScroll = null;
	private CheckBox autoScrollLog;
    private static final Pattern PATTERN_OK = Pattern.compile("\\b(OK)\\b",Pattern.CASE_INSENSITIVE);
    private static final String TAG = LogFragment.class.getName();

    public LogFragment()
    {
        // Required empty public constructor
    }

	@Override
	public String logTag(){
		return TAG;
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        logText = (TextView)view.findViewById(R.id.text);
        logScroll = (ScrollView)view.findViewById(R.id.scroll);
		autoScrollLog = (CheckBox)view.findViewById(R.id.log_auto_scroll);
		autoScrollLog.setChecked(MeshApplication.getAutoScrollLog());
		autoScrollLog.setOnClickListener(autoScrollLogClickListener);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        clearLog();
        addLogItems(Logger.all());
        update();
    }

    @Override
    public void update()
    {
        addLogItems(Logger.flush());
    }

    public void clearLog()
    {
        logText.setText("");
    }

    private void addLogItems(List<LoggerItem> items)
    {
        StringBuilder sb = new StringBuilder();
        for (LoggerItem item : items)
        {
            sb.append("<font color=\"#AAAAAA\"><i>"+ item.getTimestampString() +"</i></font> ");

			switch (item.getLevel())
            {
                case Logger.LOG_ERROR: sb.append("<b><font color=\"#CC0000\">"); break;
                case Logger.LOG_WARNING: sb.append("<font color=\"#FF9933\">"); break;
            }
            String message = item.getMessage();
            message = PATTERN_OK.matcher(message).replaceAll("<font color=\"#009933\">OK</font>");
            sb.append(message);
            switch (item.getLevel())
            {
                case Logger.LOG_ERROR: sb.append("</font></b>"); break;
                case Logger.LOG_WARNING: sb.append("</font>"); break;
            }
            sb.append("<br>\n");
        }
        logText.append(Html.fromHtml(sb.toString()));
		if (autoScrollLog.isChecked())
        	logScroll.post(new Runnable() {
            @Override
            public void run() {
                logScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

	private View.OnClickListener autoScrollLogClickListener = new View.OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			if (view != autoScrollLog)
				return;
			MeshApplication.setAutoScrollLog(autoScrollLog.isChecked());
			if (autoScrollLog.isChecked())
				logScroll.post(new Runnable() {
					@Override
					public void run() {
						logScroll.fullScroll(View.FOCUS_DOWN);
					}
				});
		}
	};
}
