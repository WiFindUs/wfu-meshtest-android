package com.wifindus.meshtester.interfaces;

import com.wifindus.meshtester.MeshService;

/**
 * Created by marzer on 25/04/2014.
 */
public interface MeshServiceSubscriber
{
    public MeshService service();
    public boolean serviceReady();
}
