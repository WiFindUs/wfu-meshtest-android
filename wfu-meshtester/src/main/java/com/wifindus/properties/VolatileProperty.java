package com.wifindus.properties;

import android.os.SystemClock;

/**
 * Created by marzer on 20/02/2015.
 */
public class VolatileProperty<T> implements VolatilePropertyProxy
{
	protected T value;
	protected final String format;
	private final long timeout;
	private long lastTimeout;
	private final String nullFormat;

	public VolatileProperty(T value, String format, long timeout)
	{
		if (format == null || (format = format.trim()).length() == 0)
			throw new IllegalArgumentException("Argument 'format' cannot be null or blank.");
		this.format = format;
		this.timeout = timeout;
		this.value = value;
		this.lastTimeout = -1; //flags as dirty
		this.nullFormat = "?";
	}

	public VolatileProperty(String format, long timeout)
	{
		this(null,format,timeout);
	}

	public VolatileProperty(String format)
	{
		this(format,1000);
	}

	public final boolean isDirty()
	{
		return lastTimeout < 0 || (SystemClock.elapsedRealtime() - lastTimeout) >= timeout;
	}

	public final void setValue(T v)
	{
		if (v == null && value == null) //both null
			return;

		if (v == value) //the same instance
			return;

		if (  ((v == null && value != null) || (v != null && value == null)) //one is null
			|| !equalityCheck(v)) //neither are null, but not equal
		{
			value = v;
			lastTimeout = -1; //flags as dirty
		}
	}

	protected boolean equalityCheck(T v)
	{
		return v.equals(value);
	}

	public final T getValue()
	{
		return value;
	}

	public final Object getValueObject()
	{
		return value;
	}

	public String formatted()
	{
		return value == null ? nullFormat : String.format(format,value);
	}

	public final void clean()
	{
		lastTimeout = SystemClock.elapsedRealtime();
	}
}
