package com.wifindus.meshtester;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by marzer on 6/12/2014.
 */
public class SignalStrengthAnalyzer
{
	private int iterationCount;
	private int worstStrength = 0;
	private boolean analyzed = false;
	private Map<String, SignalStrengthData> strengths = new TreeMap<String, SignalStrengthData>();
	private SignalStrengthData worst, best;

	public SignalStrengthAnalyzer(int iterationCount)
	{
		if (iterationCount <= 0)
			throw new IllegalArgumentException("Argument 'iterationCount' must be greater than zero.'");
		this.iterationCount = iterationCount;
	}

	public SignalStrengthAnalyzer addSample(String bssid, int iteration, int strength)
	{
		if (bssid == null || bssid.length() == 0)
			throw new IllegalArgumentException("Argument 'bssid' cannot be null or blank.'");
		if (iteration < 0 || iteration >= iterationCount)
			throw new IllegalArgumentException("Argument 'iteration' must be between 0 and "+iterationCount+" (iterationCount), inclusive.'");
		if (analyzed)
			throw new IllegalStateException("This instance of SignalStrengthAnalyzer has already been finalized by analyze().");

		//track worst strength
		if (strength < worstStrength)
			worstStrength = strength;

		//get individual strength data (create if new)
		SignalStrengthData strengthData = strengths.get(bssid);
		if (strengthData == null)
			strengths.put(bssid, strengthData = new SignalStrengthData(bssid, iterationCount));

		//pass to data
		strengthData.addSample(iteration, strength);

		return this;
	}

	public SignalStrengthAnalyzer analyze()
	{
		if (analyzed)
			return this;

		//replace missing data with worst signal and calculate means
		for (Map.Entry<String, SignalStrengthData> entry : strengths.entrySet())
		{
			SignalStrengthData current = entry.getValue();
			int currentMean = current.analyze(worstStrength).getMean();
			if (best == null || currentMean > best.getMean())
				best = current;
			if (worst == null || currentMean < worst.getMean())
				worst = current;
		}

		analyzed = true;
		return this;
	}

	public SignalStrengthData getBest()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the best mean sample result.");
		return best;
	}

	public SignalStrengthData getWorst()
	{
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to determine the worst mean sample result.");
		return best;
	}

	public SignalStrengthData getByBSSID(String bssid)
	{
		if (bssid == null || bssid.length() == 0)
			throw new IllegalArgumentException("Argument 'bssid' cannot be null or blank.'");
		if (!analyzed)
			throw new IllegalStateException("You must first call analyze() to return an analyzed sample result.");
		return strengths.get(bssid);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Analyzed signal strengths:\n");
		if (strengths.size() == 0)
			sb.append("  ** No sample data **\n");
		else
		{
			for (Map.Entry<String, SignalStrengthData> entry : strengths.entrySet())
			{
				SignalStrengthData current = entry.getValue();
				sb.append(current.toString());
				if (analyzed && strengths.size() > 1)
				{
					if (best == current)
						sb.append(" (best)");
					else if (worst == current)
						sb.append(" (worst)");
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}


}
