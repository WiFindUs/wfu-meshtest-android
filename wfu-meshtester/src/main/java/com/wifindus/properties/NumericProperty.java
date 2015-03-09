package com.wifindus.properties;

/**
 * Created by marzer on 20/02/2015.
 */
public class NumericProperty<T extends Number> extends VolatileProperty<T>
{
	protected final T epsilon;

	public NumericProperty(T value, String format, long timeout, T epsilon)
	{
		super(value, format, timeout);
		if (epsilon == null)
			throw new IllegalArgumentException("Argument 'epsilon' cannot be null.");
		this.epsilon = epsilon;
	}
}
