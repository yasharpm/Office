package com.yashoid.office.task;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yashar on 9/9/2017.
 */

class TaskExecutor extends AbstractExecutorService {

    private TaskManager mTaskManager;
    private String mSection;
    private int mPriority;

    private boolean mIsShutdown = false;

    private ArrayList<RunnableHolder> mPendingTasks;
    private ArrayList<RunnableHolder> mRunningTasks;
    private Object mTaskLock = new Object();

    private Semaphore mTerminationSemaphore = new Semaphore(0);

    protected TaskExecutor(TaskManager taskManager, String section, int priority) {
        mTaskManager = taskManager;

        mSection = section;
        mPriority = priority;

        mPendingTasks = new ArrayList<>();
        mRunningTasks = new ArrayList<>();
    }

    @Override
    public void shutdown() {
        shutdownNow();
    }

    @NonNull
    @Override
    public List<Runnable> shutdownNow() {
        synchronized (mTaskLock) {
            if (isShutdown()) {
                return new ArrayList<>(0);
            }

            mIsShutdown = true;

            ArrayList<Runnable> canceledTasks = new ArrayList<>(mPendingTasks.size());

            ListIterator<RunnableHolder> runnableListIterator = mPendingTasks.listIterator();

            while (runnableListIterator.hasNext()) {
                RunnableHolder runnable = runnableListIterator.next();

                if (mTaskManager.cancelTask(mSection, runnable, mPriority)) {
                    runnableListIterator.remove();

                    canceledTasks.add(runnable.runnable);
                }
            }

            checkTermination();

            return canceledTasks;
        }
    }

    @Override
    public boolean isShutdown() {
        return mIsShutdown;
    }

    @Override
    public boolean isTerminated() {
        synchronized (mTaskLock) {
            return mPendingTasks.isEmpty() && mRunningTasks.isEmpty();
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (isTerminated()) {
            return true;
        }

        return mTerminationSemaphore.tryAcquire(1, timeout, unit);
    }

    private void checkTermination() {
        if (isShutdown() && isTerminated()) {
            mTerminationSemaphore.release(1);
        }
    }

    @Override
    public void execute(Runnable command) {
        synchronized (mTaskLock) {
            if (isShutdown()) {
                return;
            }

            RunnableHolder runnableHolder = new RunnableHolder(command);

            mPendingTasks.add(runnableHolder);

            mTaskManager.runTask(mSection, runnableHolder, mPriority);
        }
    }

    private class RunnableHolder implements Runnable {

        private Runnable runnable;

        private RunnableHolder(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            synchronized (mTaskLock) {
                mPendingTasks.remove(this);
                mRunningTasks.add(this);
            }

            runnable.run();

            synchronized (mTaskLock) {
                mRunningTasks.remove(this);

                checkTermination();
            }
        }

    }

}
