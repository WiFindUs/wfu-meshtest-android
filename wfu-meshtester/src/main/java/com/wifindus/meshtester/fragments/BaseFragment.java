package com.wifindus.meshtester.fragments;

        import android.content.Context;
        import android.support.v4.app.Fragment;

        import com.wifindus.meshtester.MeshApplication;
        import com.wifindus.meshtester.MeshService;
        import com.wifindus.logs.LogSender;

public abstract class BaseFragment extends Fragment implements LogSender
{
    public BaseFragment() { }

    public MeshService service()
    {
        return MeshApplication.getMeshService();
    }

    public Context logContext() { return this.getActivity(); }

    public abstract void update();
}
