package com.wifindus.properties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
* Created by marzer on 20/02/2015.
	*/
public class VolatilePropertyList
{
	private final ConcurrentHashMap<String, VolatilePropertyProxy> properties
		= new ConcurrentHashMap<String, VolatilePropertyProxy>();
	private final CopyOnWriteArrayList<String> blacklist = new CopyOnWriteArrayList<String>();
	private final String separator;
	private final String format;

	public VolatilePropertyList(String format, String separator)
	{
		if (format == null || (format = format.trim()).length() == 0)
			throw new IllegalArgumentException("Argument 'format' cannot be null or blank.");
		if (separator == null || (separator = separator.trim()).length() == 0)
			throw new IllegalArgumentException("Argument 'separator' cannot be null or blank.");
		this.format = format;
		this.separator = separator;
	}

	public VolatilePropertyList(String format)
	{
		this(format, "|");
	}

	public VolatilePropertyList()
	{
		this("%s:%s");
	}

	public void addProperty(String key, VolatilePropertyProxy property)
	{
		if (key == null || (key = key.trim()).length() == 0)
			throw new IllegalArgumentException("Argument 'key' cannot be null or blank.");
		if (property == null)
			throw new IllegalArgumentException("Argument 'property' cannot be null.");
		if (properties.containsKey(key))
			throw new IllegalArgumentException("Argument 'key' already appears in key list.");
		if (properties.containsValue(property))
			throw new IllegalArgumentException("Argument 'property' already appears in property list.");
		properties.put(key, property);
	}

	public VolatilePropertyProxy getPropertyProxy(String key)
	{
		if (key == null || (key = key.trim()).length() == 0)
			return null;
		return properties.get(key);
	}

	public <T> VolatileProperty<T> getProperty(String key)
	{
		VolatilePropertyProxy proxy = getPropertyProxy(key);
		if (proxy == null)
			return null;
		try
		{
			return (VolatileProperty<T>) proxy;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public String flushList(boolean printCleanValues, boolean printDirtyValues, boolean cleanDirtyValues)
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, VolatilePropertyProxy> entry : properties.entrySet())
		{
			if (blacklist.contains(entry.getKey()))
				continue;
			boolean dirty = entry.getValue().isDirty();
			if (dirty && !printDirtyValues)
				continue;
			if (!dirty && !printCleanValues)
				continue;

			if (sb.length() > 0)
				sb.append(separator);
			sb.append(String.format(format, entry.getKey(), entry.getValue().formatted()));
			if (dirty && cleanDirtyValues)
				entry.getValue().clean();
		}
		return sb.toString();
	}

	public void addToBlacklist(String... keys)
	{
		if (keys == null || keys.length == 0)
			return;
		for (String k : keys)
		{
			String key = k.trim();
			if (key.length() == 0 || blacklist.contains(key))
				continue;
			blacklist.add(key);
		}
	}

	public void removeFromBlacklist(String... keys)
	{
		if (keys == null || keys.length == 0)
			return;
		for (String k : keys)
		{
			String key = k.trim();
			if (key.length() == 0 || !blacklist.contains(key))
				continue;
			blacklist.remove(key);
		}
	}
}
