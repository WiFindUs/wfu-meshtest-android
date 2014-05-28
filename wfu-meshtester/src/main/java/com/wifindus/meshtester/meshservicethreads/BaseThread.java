package com.wifindus.meshtester.meshservicethreads;

import com.wifindus.meshtester.MeshApplication;
import com.wifindus.meshtester.MeshService;
import com.wifindus.meshtester.SystemManager;
import com.wifindus.meshtester.interfaces.MeshApplicationSubscriber;
import com.wifindus.meshtester.interfaces.MeshServiceSubscriber;
import com.wifindus.meshtester.interfaces.SystemManagerSubscriber;
import com.wifindus.meshtester.interfaces.ThreadWaitCondition;

/**
 * Created by marzer on 25/04/2014.
 */
public abstract class BaseThread extends Thread implements MeshApplicationSubscriber, MeshServiceSubscriber, SystemManagerSubscriber
{
    private static volatile boolean cancelAll = false;
    private volatile boolean cancel = false;
    public static final long WAIT_LOOP_INTERVAL = 200;

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
     * Waits in a loop for the specified timeout, checking for cancelled state and condition state at each iteration.
     * @param waitCheck A condition to evaluate at each iteration.
     * @return <b>true</b> when waitCheck's ConditionCheck() evaluates to true, <b>false</b> if the timeout was reached or the wait was interrupted (e.g. due to thread cancellation).
     */
    protected final boolean waiteval(ThreadWaitCondition waitCheck)
    {
        long counter = 0;
        long currentTimeout = waitCheck.timeoutLength();
        long iteration_length = waitCheck.iterationLength();

        while(currentTimeout < 0 || counter < currentTimeout)
        {
            if (waitCheck.conditionCheck())
                return true;

            if (!safesleep(iteration_length) || isCancelled())
                return false;
            waitCheck.postWaitUpdate();
            counter += iteration_length;

            long newTimeout = waitCheck.timeoutLength();
            if (newTimeout != currentTimeout)
            {
                counter = 0;
                currentTimeout = newTimeout;
            }
        }

        return false;
    }

    /**
     * Waits in a loop for the specified timeout, checking for cancelled state at each iteration. The same as {@link #waitloop(long,long)}, but uses {@link #WAIT_LOOP_INTERVAL} as the iteration_timeout parameter.
     * @param timeout A length of time to wait for (at least), in milliseconds.
     * @return <b>true</b> if the timeout was reached without interruption (eg due to thread cancellation), <b>false</b> otherwise.
     */
    protected final boolean waitloop(long timeout)
    {
        return waitloop(timeout, WAIT_LOOP_INTERVAL);
    }

    /**
     * Waits in a loop for the specified timeout, checking for cancelled state at each iteration.
     * @param timeout A length of time to wait for (at least) in total (ms).
     * @param iteration_timeout A length of time to wait for (at least) at each iteration of the loop (ms).
     * @return <b>true</b> if the timeout was reached without interruption (eg due to thread cancellation), <b>false</b> otherwise.
     */
    protected final boolean waitloop(long timeout, long iteration_timeout)
    {
        long counter = 0;
        while(timeout < 0 || counter < timeout)
        {
            if (!safesleep(iteration_timeout) || isCancelled())
                return false;
            counter += iteration_timeout;
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
     * Determines how long the thread should wait before trying it's main iteration loop again. Threads may wish to override this depending on current hardware state etc.
     * @return The length of time in milliseconds.
     */
    protected long iterationInterval()
    {
        return 5000;
    }

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
            if (!waitloop(iterationInterval()))
                break;
        }
        cleanup();
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

    @Override
    public SystemManager systems()
    {
        return app().systems();
    }
}
