package com.wifindus.meshtester.fragments;

        import android.content.Context;
        import android.support.v4.app.Fragment;

        import com.wifindus.meshtester.MeshApplication;
        import com.wifindus.meshtester.MeshService;
        import com.wifindus.meshtester.logs.LogSender;

public abstract class BaseFragment extends Fragment implements LogSender
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

    public Context logContext() { return this.getActivity(); }
}
