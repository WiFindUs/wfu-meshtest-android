package com.wifindus.properties;

/**
 * Created by marzer on 20/02/2015.
 */
public interface VolatilePropertyProxy
{
	public boolean isDirty();
	public String formatted();
	public void clean();
	public Object getValueObject();
}
