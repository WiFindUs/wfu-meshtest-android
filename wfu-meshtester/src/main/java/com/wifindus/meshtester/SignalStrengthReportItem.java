package com.wifindus.meshtester;

import android.location.Location;

/**
 * Created by Mallik on 6/12/2014.
 */
public class SignalStrengthReportItem
{
    private SignalStrengthData data;
    private Location location;
    private long timestamp;

    public SignalStrengthReportItem(SignalStrengthData data, Location location)
    {
        timestamp = System.currentTimeMillis();
        this.data = data;
        this.location = location;
    }

    public static String headers()
    {
        return  "BSSID"
            + ",\t" + "Best (dbm)"
            + ",\t" + "Mean (dbm)"
            + ",\t" + "Worst (dbm)"
            + ",\t" + "Tier"
            + ",\t" + "Count"
            + ",\t" + "Failed"
            + ",\t" + "Latitude"
            + ",\t" + "Longitude"
            + ",\t" + "Device ID";
    }

    @Override
    public String toString() {
        return data.getBSSID()
                + ",\t" + data.getBest()
                + ",\t" + data.getMean()
                + ",\t" + data.getWorst()
                + ",\t" + data.getTier()
                + ",\t" + data.getCount()
                + ",\t" + data.getMissingCount()
                + ",\t" + (location == null ? " " : location.getLatitude())
                + ",\t" + (location == null ? " " : location.getLongitude())
                + ",\t" + MeshApplication.getID();

    }
}
