package com.wifindus.meshtester;

import android.location.Location;

/**
 * Created by Mallik on 6/12/2014.
 */
public class SignalStrengthReportItem
{
    private SignalStrengthData data;
    private Double latitude, longitude;

    public SignalStrengthReportItem(SignalStrengthData data, Double latitude, Double longitude)
    {
        this.data = data;
        this.latitude = latitude;
		this.longitude = longitude;
    }

	public SignalStrengthReportItem(SignalStrengthData data)
	{
		this(data,null,null);
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
                + ",\t" + (latitude == null ? " " : latitude)
                + ",\t" + (longitude == null ? " " : longitude)
                + ",\t" + MeshApplication.getID();

    }
}
