package com.wifindus.meshtester.fragments;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.wifindus.meshtester.R;
import com.wifindus.meshtester.logs.Logger;
import com.wifindus.meshtester.logs.LoggerItem;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LogFragment extends BaseFragment
{
    private TextView logText = null;
    private ScrollView logScroll = null;
    private static final Pattern PATTERN_OK = Pattern.compile("\\b(OK)\\b",Pattern.CASE_INSENSITIVE);

    public LogFragment()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        logText = (TextView)view.findViewById(R.id.log_text);
        logScroll = (ScrollView)view.findViewById(R.id.log_scroll);
        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        logText.setText("");
        addLogItems(Logger.all());
        addLogItems(Logger.flush());
    }

    public void updateLogItems()
    {
        addLogItems(Logger.flush());
    }

    private void addLogItems(List<LoggerItem> items)
    {
        StringBuilder sb = new StringBuilder();
        for (LoggerItem item : items)
        {
            switch (item.getLevel())
            {
                case Logger.LOG_ERROR: sb.append("<b><font color=\"red\">"); break;
                case Logger.LOG_WARNING: sb.append("<font color=\"yellow\">"); break;
            }
            String message = item.getMessage();
            message = PATTERN_OK.matcher(message).replaceAll("<font color=\"green\">OK</font>");
            sb.append(message);
            switch (item.getLevel())
            {
                case Logger.LOG_ERROR: sb.append("</font></b>"); break;
                case Logger.LOG_WARNING: sb.append("</font>"); break;
            }
            sb.append("<br>\n");
        }
        logText.append(Html.fromHtml(sb.toString()));
        logScroll.fullScroll(ScrollView.FOCUS_DOWN);
    }
}
