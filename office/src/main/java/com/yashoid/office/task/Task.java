package com.yashoid.office.task;

import com.yashoid.office.util.FlexiblePool;

/**
 * Created by Yashar on 4/13/2017.
 */

public class Task implements Runnable {

    public static final int STATE_CREATED = 0;
    public static final int STATE_PROCESSED = 1;
    public static final int STATE_POSTED = 2;
    public static final int STATE_IN_EXECUTION = 3;
    public static final int STATE_EXECUTED = 4;
    public static final int STATE_RELEASED = 5;

    private static FlexiblePool<Task> mPool = null;

    public interface TaskPerformer {

        void performTask(Task task);

    }

    public static Task obtain(String section, TaskPerformer performer, long id) {
        return obtain(section, performer, id, 0);
    }

    public static Task obtain(String section, TaskPerformer performer, long id, int priority) {
        Task task = getPool().acquire();

        task.setup(section, performer, id, priority);

        return task;
    }

    public static void release(Task task) {
        getPool().release(task);
    }

    private static FlexiblePool<Task> getPool() {
        if (mPool == null) {
            mPool = new FlexiblePool<Task>() {

                @Override
                protected Task newInstance() {
                    return new Task();
                }

                @Override
                protected void onInstanceReleased(Task instance) {
                    instance.mPerformer = null;
//                    instance.mIsScheduled = false;
                    instance.mState = STATE_RELEASED;
                }

            };
        }

        return mPool;
    }

    private String mSection;

    private TaskPerformer mPerformer = null;
    private long mId;

    private int mPriority;

//    private boolean mIsScheduled = false;
    private int mState;

    private Task() {

    }

    private void setup(String section, TaskPerformer performer, long id, int priority) {
        mSection = section;

        mPerformer = performer;
        mId = id;

        mPriority = priority;

        mState = STATE_CREATED;
    }

    public long getId() {
        return mId;
    }

    protected String getSection() {
        return mSection;
    }

    protected int getPriority() {
        return mPriority;
    }

    @Override
    public void run() {
        if (mPerformer == null) {
            throw new IllegalStateException("Task has not been setup properly.");
        }

        mPerformer.performTask(this);

        release(this);
    }

//    public synchronized void setScheduled(boolean scheduled) {
//        mIsScheduled = scheduled;
//    }
//
//    public synchronized boolean isScheduled() {
//        return mIsScheduled;
//    }

    public synchronized void setState(int state) {
        mState = state;
    }

    public synchronized int getState() {
        return mState;
    }

}
