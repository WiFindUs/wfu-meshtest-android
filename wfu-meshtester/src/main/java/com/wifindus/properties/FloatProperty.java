package com.wifindus.properties;

/**
 * Created by marzer on 20/02/2015.
 */
public class FloatProperty extends NumericProperty<Float>
{
	public FloatProperty(Float value, String format, long timeout, Float epsilon)
	{
		super(value, format, timeout, epsilon);
	}

	public FloatProperty(Float value, String format, long timeout)
	{
		super(value, format, timeout, 0.000001f);
	}

	@Override
	protected boolean equalityCheck(Float v)
	{
		if (super.equalityCheck(v))
			return true;

		if ((v == null && value != null) || (v != null && value == null)) //one is null
			return false;

		if (Float.isNaN(value) && Float.isNaN(v))
			return true;

		if (Float.isInfinite(value) && Float.isInfinite(v))
			return true;

		return Math.abs(v-value) < epsilon;
	}
}
