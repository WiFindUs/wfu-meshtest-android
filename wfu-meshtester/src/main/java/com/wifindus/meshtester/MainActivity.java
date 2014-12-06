package com.wifindus.meshtester;

import java.util.Locale;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuItem;

import com.wifindus.meshtester.fragments.LogFragment;
import com.wifindus.meshtester.fragments.PingFragment;
import com.wifindus.meshtester.fragments.UserFragment;
import com.wifindus.meshtester.fragments.StatusFragment;
import com.wifindus.logs.LogSender;
import com.wifindus.logs.Logger;


public class MainActivity extends FragmentActivity
    implements ActionBar.TabListener, LogSender
{
    public static final int PAGE_USER = 0;
    public static final int PAGE_STATUS = 1;
    public static final int PAGE_PING = 2;
    public static final int PAGE_LOG = 3;
    private static final String TAG = MainActivity.class.getName();

    private MeshActivityReceiver meshActivityReceiver = null;
    private UserFragment userFragment = null;
    private StatusFragment statusFragment = null;
    private LogFragment logFragment = null;
    private PingFragment pingFragment = null;
    private SectionsPagerAdapter sectionsPagerAdapter = null;
    private ViewPager viewPager = null;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                actionBar.newTab()
                    .setText(sectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }

        //set up the broadcast receiver
        meshActivityReceiver = new MeshActivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Logger.ACTION_UPDATE_LOG);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_CONNECTION_STATE);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_LOCATION);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_MESH_ADDRESS);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_CLEANED);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_USER);
        intentFilter.addAction(MeshApplication.ACTION_UPDATE_PINGS);
		intentFilter.addAction(MeshApplication.ACTION_UPDATE_BATTERY);
        registerReceiver(meshActivityReceiver, intentFilter);

        //create the background service
        if (MeshApplication.getMeshService() == null)
            startMeshService();

        setTitle(getTitle() + " " + MeshApplication.getVersion());
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(meshActivityReceiver);
        super.onDestroy();
    }

    /////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////////////////////////////////////////

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if (id == R.id.action_exit)
        {
            stopMeshService();
            Logger.clear();
            if (logFragment != null)
                logFragment.clearLog();
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            */
			this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public MeshService service()
    {
        return MeshApplication.getMeshService();
    }

    public boolean serviceReady()
    {
        return service() != null && service().isReady();
    }

    public SystemManager systems()
    {
        return MeshApplication.systems();
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


    /////////////////////////////////////////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////////////////////////////////////////

    private void startMeshService()
    {
        Logger.i(this, "Starting mesh service...");
        Intent serviceIntent = new Intent(this, MeshService.class);
        startService(serviceIntent);
    }

    private void stopMeshService()
    {
        Logger.i(this, "Stopping mesh service...");
        stopService(new Intent(this, MeshService.class));
    }

    private class MeshActivityReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context arg0, Intent arg1)
        {
            if (arg1.getAction().equals(Logger.ACTION_UPDATE_LOG))
            {
                if (logFragment != null)
                    logFragment.update();
            }
            else if (arg1.getAction().equals(MeshApplication.ACTION_UPDATE_CONNECTION_STATE)
             || arg1.getAction().equals(MeshApplication.ACTION_UPDATE_LOCATION)
             || arg1.getAction().equals(MeshApplication.ACTION_UPDATE_MESH_ADDRESS)
             || arg1.getAction().equals(MeshApplication.ACTION_UPDATE_CLEANED)
			 || arg1.getAction().equals(MeshApplication.ACTION_UPDATE_BATTERY))
            {
                if (statusFragment != null)
                    statusFragment.update();
            }
            else if (arg1.getAction().equals(MeshApplication.ACTION_UPDATE_PINGS))
            {
                if (pingFragment != null)
                    pingFragment.update();
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case PAGE_USER: return userFragment = new UserFragment();
                case PAGE_STATUS: return statusFragment = new StatusFragment();
                case PAGE_PING: return pingFragment = new PingFragment();
                case PAGE_LOG: return logFragment = new LogFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position)
            {
                case PAGE_USER:
                    return getString(R.string.title_user).toUpperCase(l);
                case PAGE_STATUS:
                    return getString(R.string.title_status).toUpperCase(l);
                case PAGE_LOG:
                    return getString(R.string.title_log).toUpperCase(l);
                case PAGE_PING:
                    return getString(R.string.title_ping).toUpperCase(l);
            }
            return null;
        }
    }
}
