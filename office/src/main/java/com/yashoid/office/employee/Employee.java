package com.yashoid.office.employee;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;

/**
 * Created by Yashar on 9/20/2016.
 */
public class Employee {

    private EmployeeManager mManager;

    private String mName;

    private boolean mHasStartedWorking = false;

    private Handler mTaskSender;

    private HashMap<Runnable, Object> mTaskLocks = new HashMap<>(8);

    protected Employee(EmployeeManager manager, String name) {
        mManager = manager;

        mName = name;
    }

    public void assignTask(Runnable task) {
        mManager.onEmployeeIsBusy(this);

        if (!mHasStartedWorking) {
            new EmployeeThread().start();

            mHasStartedWorking = true;
        }

        makeSureEmployeeHasStartedWorking();

        mTaskSender.removeCallbacks(mNotifyFreeTask);
        mTaskSender.post(task);
        mTaskSender.post(mNotifyFreeTask);
    }

    public void assignTaskAndWait(final Runnable task) {
        mManager.onEmployeeIsBusy(this);

        if (!mHasStartedWorking) {
            new EmployeeThread().start();

            mHasStartedWorking = true;
        }

        makeSureEmployeeHasStartedWorking();

        final Object waitLock = new Object();

        synchronized (mTaskLocks) {
            mTaskLocks.put(task, waitLock);
        }

        mTaskSender.removeCallbacks(mNotifyFreeTask);
        mTaskSender.post(task);
        mTaskSender.post(new Runnable() {

            @Override
            public void run() {
                synchronized (mTaskLocks) {
                    mTaskLocks.remove(task);
                }

                synchronized (waitLock) {
                    waitLock.notifyAll();
                }
            }

        });

        synchronized (waitLock) {
            try {
                waitLock.wait();
            } catch (InterruptedException e) { }
        }

        mTaskSender.post(mNotifyFreeTask);
    }

    public boolean alsoWaitForTask(Runnable task) {
        synchronized (mTaskLocks) {
            Object waitLock = mTaskLocks.get(task);

            if (waitLock == null) {
                return false;
            }

            synchronized (waitLock) {
                try {
                    waitLock.wait();
                } catch (InterruptedException e) { }
            }

            return true;
        }
    }

    public void cancelTask(Runnable task) {
        makeSureEmployeeHasStartedWorking();

        mTaskSender.removeCallbacks(task);
    }

    private void makeSureEmployeeHasStartedWorking() {
        if (mTaskSender == null) {
            synchronized (this) {
                if (mTaskSender == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) { }
                }
            }
        }
    }

    private Runnable mNotifyFreeTask = new Runnable() {

        @Override
        public void run() {
            mManager.onEmployeeIsFree(Employee.this);
        }

    };

    protected void relieve() {
        if (mHasStartedWorking) {
            makeSureEmployeeHasStartedWorking();

            mTaskSender.removeCallbacks(mNotifyFreeTask);

            if (Build.VERSION.SDK_INT >= 18) {
                mTaskSender.getLooper().quitSafely();
            }
            else {
                mTaskSender.getLooper().quit();
            }
        }
    }

    private class EmployeeThread extends Thread {

        @Override
        public void run() {
            if (mName != null) {
                Thread.currentThread().setName(mName);
            }

            Looper.prepare();

            synchronized (Employee.this) {
                mTaskSender = new Handler();

                Employee.this.notifyAll();
            }

            Looper.loop();
        }

    }

}
