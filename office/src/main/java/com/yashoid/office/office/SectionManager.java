package com.yashoid.office.office;

import android.util.SparseArray;

import com.yashoid.office.employee.Employee;
import com.yashoid.office.employee.EmployeeManager;

import java.util.ArrayList;

/**
 * Created by Yashar on 4/14/2017.
 */

public class SectionManager {

    private EmployeeManager mEmployeeManager;

    private SparseArray<ArrayList<Runnable>> mPendingTasks = new SparseArray<>(4);
    private int mPendingTasksCount = 0;

    protected SectionManager(String name, int employeeCount) {
        mEmployeeManager = new EmployeeManager(name, employeeCount);
    }

    protected void assignTask(Runnable task, int priority) {
        Employee employee = mEmployeeManager.findFreeEmployee(false);

        if (employee != null) {
            employee.assignTask(task);
            return;
        }

        addTaskToPendingTasks(task, priority);

        mEmployeeManager.notifyWhenFreeEmployeeIsAvailable(mOnFreeEmployeeAvailableListener);
    }

    protected void assignTaskAndWait(final Runnable task, int priority) {
        Employee employee = mEmployeeManager.findFreeEmployee(false);

        if (employee != null) {
            employee.assignTaskAndWait(task);
            return;
        }

        final Object lock = new Object();

        Runnable waitingTask = new Runnable() {

            @Override
            public void run() {
                task.run();

                synchronized (lock) {
                    lock.notifyAll();
                }
            }

        };

        addTaskToPendingTasks(waitingTask, priority);

        mEmployeeManager.notifyWhenFreeEmployeeIsAvailable(mOnFreeEmployeeAvailableListener);

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) { }
        }
    }

    /**
     *
     * @param task
     * @param priority
     * @return true if the task has truly canceled.
     */
    protected boolean cancelTask(Runnable task, int priority) {
        synchronized (this) {
            task = takeSpecificTask(task, priority);

            if (task != null) {
                return true;
            }

            return false;
        }
    }

    /**
     *
     * @param task
     * @return true if the task has truly canceled.
     */
    protected boolean cancelTask(Runnable task) {
        synchronized (this) {
            task = takeSpecificTask(task);

            if (task != null) {
                return true;
            }

            return false;
        }
    }

    protected void close() {
        synchronized (this) {
            mEmployeeManager.relieveEmployees();

            mPendingTasks.clear();

            mPendingTasksCount = 0;
        }
    }

    private void addTaskToPendingTasks(Runnable task, int priority) {
        synchronized (this) {
            getPendingTasks(priority).add(task);

            mPendingTasksCount++;
        }
    }

    private ArrayList<Runnable> getPendingTasks(int priority) {
        ArrayList<Runnable> pendingTasks = mPendingTasks.get(priority);

        if (pendingTasks == null) {
            pendingTasks = new ArrayList<>(4);

            mPendingTasks.put(priority, pendingTasks);
        }

        return pendingTasks;
    }

    private EmployeeManager.OnFreeEmployeeAvailableListener mOnFreeEmployeeAvailableListener =
            new EmployeeManager.OnFreeEmployeeAvailableListener() {

        @Override
        public void onFreeEmployeeAvailable() {
            synchronized (this) {
                if (mPendingTasksCount == 0) {
                    return;
                }

                Runnable task = takeTaskWithHighestPriority();

                assignTask(task, 0);

                if (mPendingTasksCount > 0) {
                    mEmployeeManager.notifyWhenFreeEmployeeIsAvailable(mOnFreeEmployeeAvailableListener);
                }
            }
        }

    };

    private Runnable takeTaskWithHighestPriority() {
        synchronized (this) {
            int highestPriority = mPendingTasks.keyAt(mPendingTasks.size() - 1);

            ArrayList<Runnable> pendingTasks = mPendingTasks.get(highestPriority);

            Runnable task = pendingTasks.get(0);

            pendingTasks.remove(task);

            mPendingTasksCount--;

            if (pendingTasks.size() == 0) {
                mPendingTasks.remove(highestPriority);
            }

            return task;
        }
    }

    private Runnable takeSpecificTask(Runnable task) {
        synchronized (this) {
            for (int i=0; i<mPendingTasks.size(); i++) {
                int key = mPendingTasks.keyAt(i);
                ArrayList<Runnable> pendingTasks = mPendingTasks.get(key);

                if (pendingTasks.contains(task)) {
                    pendingTasks.remove(task);

                    if (pendingTasks.size() == 0) {
                        mPendingTasks.remove(key);
                    }

                    mPendingTasksCount--;

                    return task;
                }
            }

            return null;
        }
    }

    private Runnable takeSpecificTask(Runnable task, int priority) {
        synchronized (this) {
            ArrayList<Runnable> pendingTasks = mPendingTasks.get(priority);

            if (pendingTasks != null && pendingTasks.contains(task)) {
                pendingTasks.remove(task);

                if (pendingTasks.size() == 0) {
                    mPendingTasks.remove(priority);
                }

                mPendingTasksCount--;

                return task;
            }

            return null;
        }
    }

}
