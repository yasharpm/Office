package com.yashoid.office;

import com.yashoid.office.task.Task;
import com.yashoid.office.task.TaskManager;

import java.util.ArrayList;

/**
 * Created by Yashar on 4/19/2017.
 */

public abstract class AsyncOperation extends Operation {

    private String mBackgroundSection;

    private long mPreExecuteTaskId;
    private long mBackgroundTaskId;
    private long mPostExecuteTaskId;

    private ArrayList<Object> mPublishes = new ArrayList<>(10);

    private boolean mCanceled = false;
    private boolean mExecuted = false;
    private boolean mFinishedExecuting = false;

    private Object mStateLock = new Object();

    public AsyncOperation(TaskManager taskManager, String backgroundSection) {
        super(taskManager);

        if (backgroundSection == null) {
            throw new IllegalArgumentException("Background section name can not be null.");
        }

        mBackgroundSection = backgroundSection;
    }

    public void execute() {
        synchronized (mStateLock) {
            if (mExecuted) {
                throw new IllegalStateException("AsyncOperation is already executed.");
            }

            mExecuted = true;
        }

        Task preExecuteTask = newTask(TaskManager.MAIN);
        Task backgroundTask = newTask(mBackgroundSection);
        Task postExecuteTask = newTask(TaskManager.MAIN);

        mPreExecuteTaskId = preExecuteTask.getId();
        mBackgroundTaskId = backgroundTask.getId();
        mPostExecuteTaskId = postExecuteTask.getId();

        runTask(preExecuteTask);
        scheduleTask(backgroundTask, preExecuteTask);
        scheduleTask(postExecuteTask, backgroundTask);
    }

    public boolean isExecuted() {
        synchronized (mStateLock) {
            return mExecuted;
        }
    }

    public boolean isCanceled() {
        synchronized (mStateLock) {
            return mCanceled;
        }
    }

    public boolean cancel() {
        synchronized (mStateLock) {
            if (!mExecuted) {
                throw new IllegalStateException("AsyncOperation not executed yet to be canceled.");
            }

            if (mFinishedExecuting) {
                return false;
            }

            mCanceled = true;

            cancelAllTasks();

            return true;
        }
    }

    @Override
    protected void onPerformTask(long taskId) {
        synchronized (mStateLock) {
            if (mCanceled) {
                return;
            }
        }

        if (taskId == mPreExecuteTaskId) {
            onPreExecute();
        }
        else if (taskId == mBackgroundTaskId) {
            doInBackground();
        }
        else if (taskId == mPostExecuteTaskId) {
            synchronized (mStateLock) {
                if (mCanceled) {
                    return;
                }

                mFinishedExecuting = true;
            }

            onPostExecute();
        }
        else {
            synchronized (mPublishes) {
                Object object = mPublishes.remove(0);

                onProgressUpdate(object);
            }
        }
    }

    protected void publishProgress(Object object) {
        synchronized (mPublishes) {
            mPublishes.add(object);
        }

        Task publishTask = newTask(TaskManager.MAIN);
        runTask(publishTask);
    }

    protected void onPreExecute() {

    }

    abstract protected void doInBackground();

    protected void onProgressUpdate(Object object) {

    }

    protected void onPostExecute() {

    }

}
