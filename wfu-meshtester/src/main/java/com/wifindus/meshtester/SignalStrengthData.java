package com.wifindus.meshtester;

/**
 * Created by marzer on 6/12/2014.
 */
public class SignalStrengthData
{
	private Integer[] strengths;
	private int meanStrength = 0, best = Integer.MIN_VALUE, worst = Integer.MAX_VALUE;
	private boolean analyzed = false;
	private int iterationCount;
	private int missingCount = -1;
	private int tier = -1;
	private String bssid;

	public SignalStrengthData(String bssid, int iterationCount)
	{
		if (bssid == null || bssid.length() == 0)
			throw new IllegalArgumentException("Argument 'bssid' cannot be null or blank.");
		if (iterationCount <= 0)
			throw new IllegalArgumentException("Argument 'iterationCount' must be greater than zero.");

		this.bssid = bssid;
		this.iterationCount = iterationCount;
		strengths = new Integer[iterationCount];
		for (int i = 0; i < iterationCount; i++)
			strengths[i] = null;
	}

	public SignalStrengthData addSample(int iteration, int strength)
	{
		if (iteration < 0 || iteration >= iterationCount)
			throw new IllegalArgumentException("Argument 'iteration' must be between 0 and "+iterationCount+" (iterationCount), inclusive.");
		if (analyzed)
			throw new IllegalStateException("This instance of SignalStrengthData has already been finalized by analyze().");

		//determine if this sample is useful (to account for spikes etc).
		//invalid ones are ignored (considered missing).
		if (strength <= -30 && strength >= -125)
			strengths[iteration] = Integer.valueOf(strength);

		return this;
	}

	public SignalStrengthData analyze(int substituteForMissing)
	{
		if (analyzed)
			return this;

		//determine mean and count missing values
		double sum = 0.0f;
		missingCount = 0;
		for (int i = 0; i < iterationCount; i++)
		{
			if (strengths[i] == null)
			{
				missingCount++;
				sum += (double) substituteForMissing;
			}
			else
			{
				sum += (double)strengths[i];
				if (strengths[i] < worst)
					worst = strengths[i];
				if (strengths[i] > best)
					best = strengths[i];
			}
		}
		meanStrength = (int)(sum/iterationCount);

		//determine tier
		if (meanStrength <= -30 && meanStrength > -67)
			tier = 4;
		else if (meanStrength > -70)
			tier = 3;
		else if (meanStrength > -80)
			tier = 2;
		else if (meanStrength > -90)
			tier = 1;
		else
			tier = 0;

		analyzed = true;
		return this;
	}

	public SignalStrengthData analyze()
	{
		return analyze(-200);
	}

	public String getBSSID()
	{
		return bssid;
	}

	public int getMean()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the mean sample.");
		return meanStrength;
	}

	public int getBest()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the best sample.");
		return best;
	}

	public int getWorst()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the worst sample.");
		return best;
	}

    public int getCount()
    {
        return strengths.length;
    }

	public int getMissingCount()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the missing sample count.");
		return missingCount;
	}

	public int getTier()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the mean signal strength tier.");
		return tier;
	}

	@Override
	public String toString()
	{
		return bssid + ": " + (analyzed ? ""+meanStrength : " not analyzed." );
	}
}
