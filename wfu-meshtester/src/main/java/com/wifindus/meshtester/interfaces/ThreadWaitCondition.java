package com.wifindus.meshtester.interfaces;

/**
 * Created by marzer on 25/04/2014.
 */
public interface ThreadWaitCondition
{
    public boolean conditionCheck();
    public void postWaitUpdate();
    public long iterationLength();
    public long timeoutLength();
}