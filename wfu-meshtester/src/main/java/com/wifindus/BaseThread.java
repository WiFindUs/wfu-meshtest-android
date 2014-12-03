package com.wifindus;

import android.content.Context;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.MeshService;
import com.wifindus.meshtester.SystemManager;
import com.wifindus.logs.LogSender;

/**
 * Created by marzer on 25/04/2014.
 */
public abstract class BaseThread extends Thread implements LogSender, TimeoutProvider
{
    private static final long WAIT_LOOP_INTERVAL = 200;
    private static volatile boolean cancelAll = false;
    private volatile boolean cancel = false;
    private volatile Context context;

    /////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////////////////////////////////////////

    public BaseThread(Context launchingContext)
    {
        context = launchingContext;
    }

    public Context logContext()
    {
        return context;
    }

    /////////////////////////////////////////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////////////////////////////////////////

    /**
     * Run event of the thread. Children cannot override it - instead they should implement their thread functionality by overriding {@link #prepare()}, {@link #iteration()}, and {@link #cleanup()}.
     */
    @Override
    public final void run()
    {
        prepare();
        while (!isCancelled())
        {
            iteration();
            if (!waitloop(this))
                break;
        }
        cleanup();
    }

    public MeshService service()
    {
        return MeshApplication.getMeshService();
    }

    public boolean serviceReady()
    {
        return service() != null && service().isReady();
    }

    public SystemManager systems()
    {
        return MeshApplication.systems();
    }

    public boolean isCancelled()
    {
        return cancelAll || cancel;
    }

    public static void cancelAllThreads()
    {
        cancelAll = true;
    }

    public void cancelThread()
    {
        cancel = true;
    }

	/**
	 * Determines how long the thread should wait before trying it's main iteration loop again. Threads may wish to override this depending on current hardware state etc.
	 * @return The length of time in milliseconds.
	 */
	public long timeoutLength() { return 5000; }

    /////////////////////////////////////////////////////////////////////
    // PROTECTED METHODS
    /////////////////////////////////////////////////////////////////////

    /**
     * Waits in a loop for the specified timeout, checking for cancelled state and condition state at each iteration.
     * @param waitCheck A condition to evaluate at each iteration.
     * @return <b>true</b> when waitCheck's ConditionCheck() evaluates to true, <b>false</b> if the timeout was reached or the wait was interrupted (e.g. due to thread cancellation).
     */
    protected final boolean waiteval(ThreadWaitCondition waitCheck, TimeoutProvider timeout)
    {
        long counter = 0;
        while(timeout.timeoutLength() < 0 || counter < timeout.timeoutLength())
        {
            if (waitCheck.conditionCheck())
                return true;
            if (!safesleep(WAIT_LOOP_INTERVAL) || isCancelled())
                return false;
            counter += WAIT_LOOP_INTERVAL;
        }
        return false;
    }

    /**
     * Waits in a loop for the specified timeout, checking for cancelled state at each iteration.
     * @param timeout A length of time to wait for (at least) in total (ms).
     * @return <b>true</b> if the timeout was reached without interruption (eg due to thread cancellation), <b>false</b> otherwise.
     */
    protected final boolean waitloop(TimeoutProvider timeout)
    {
        long counter = 0;
        while(timeout.timeoutLength() < 0 || counter < timeout.timeoutLength())
        {
            if (!safesleep(WAIT_LOOP_INTERVAL) || isCancelled())
                return false;
            counter += WAIT_LOOP_INTERVAL;
        }
        return true;
    }

    /**
     * Convenience wrapper around Thread's sleep() method to deal with InterruptedException.
     * @param timeout A length of time to wait for (at least), in milliseconds.
     * @return <b>true</b> if the timeout was reached without interruption (eg due to thread cancellation), <b>false</b> otherwise.
     */
    protected final boolean safesleep(long timeout)
    {
        try { Thread.sleep(timeout); } catch (InterruptedException e) { return false; }
        return true;
    }

    /**
     * Called before {@link #iteration()} in {@link #run()}. Children should override this to instantiate key objects etc.
    */
    protected abstract void prepare();

    /**
     * Called in a loop after {@link #prepare()} in {@link #run()}. Children should override this to perform their actual thread functionality.
     */
    protected abstract void iteration();

    /**
     * Called after {@link #iteration()} in {@link #run()}. Children should override this to perform any cleanup.
     */
    protected abstract void cleanup();
}
