package com.wifindus.meshtester.fragments;

import android.support.v4.app.Fragment;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.MeshService;
import com.wifindus.meshtester.interfaces.MeshApplicationSubscriber;
import com.wifindus.meshtester.interfaces.MeshServiceSubscriber;

public abstract class BaseFragment extends Fragment implements MeshApplicationSubscriber, MeshServiceSubscriber
{
    public BaseFragment()
    {

    }

    @Override
    public MeshApplication app()
    {
        return MeshApplication.ref();
    }

    @Override
    public MeshService service()
    {
        return app().getMeshService();
    }

    @Override
    public boolean serviceReady()
    {
        return service() != null && service().isReady();
    }
}
