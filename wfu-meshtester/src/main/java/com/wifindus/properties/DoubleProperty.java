package com.wifindus.properties;

/**
 * Created by marzer on 20/02/2015.
 */
public class DoubleProperty extends NumericProperty<Double>
{
	public DoubleProperty(Double value, String format, long timeout, Double epsilon)
	{
		super(value, format, timeout, epsilon);
	}

	public DoubleProperty(Double value, String format, long timeout)
	{
		super(value, format, timeout, 0.000000000001);
	}

	@Override
	protected boolean equalityCheck(Double v)
	{
		if (super.equalityCheck(v))
			return true;

		if (Double.isNaN(value) && Double.isNaN(v))
			return true;

		if (Double.isInfinite(value) && Double.isInfinite(v))
			return true;

		return Math.abs(v-value) < epsilon;
	}
}
