package com.wifindus.meshtester.fragments;

import android.support.v4.app.Fragment;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.MeshService;

public abstract class BaseFragment extends Fragment
{
    public BaseFragment() { }

    public MeshService service()
    {
        return MeshApplication.getMeshService();
    }

    public boolean serviceReady()
    {
        return service() != null && service().isReady();
    }
}
