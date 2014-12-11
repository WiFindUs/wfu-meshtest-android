package com.wifindus;

import android.location.Location;

public abstract class MathHelper
{
    public static double EPSILON_DOUBLE = 0.000000000001;
    public static double EPSILON_FLOAT = 0.000001;

    public static boolean equal(double a, double b)
    {
        if (Double.isNaN(a) && Double.isNaN(b))
            return true;

        if (Double.isInfinite(a) && Double.isInfinite(b))
            return true;

        return Math.abs(a-b) < EPSILON_DOUBLE;
    }

    public static boolean equal(float a, float b)
    {
        if (Float.isNaN(a) && Float.isNaN(b))
            return true;

        if (Float.isInfinite(a) && Float.isInfinite(b))
            return true;

        return Math.abs(a-b) < EPSILON_FLOAT;
    }

    public static double lerp(double a, double b, double f)
    {
        return	(1.0 - f)*a + f*b;
    }

    public static double coserp(double a, double b, double f)
    {
        double prc = (1.0 - Math.cos(f * Math.PI)) * 0.5;
        return a *(1.0 - prc) + b * prc;
    }

	public static boolean isEmptyLocation(Location loc)
	{
		if (loc == null)
			return true;
		return equal(loc.getLatitude(),0.0)
			&& equal(loc.getLongitude(),0.0)
			&& (!loc.hasAccuracy() || equal(loc.getAccuracy(),0.0f))
			&& (!loc.hasAltitude() || equal(loc.getAltitude(),0.0f));
	}
}
