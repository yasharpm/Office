package com.yashoid.office.office;

import android.os.Handler;

import com.yashoid.office.employee.Employee;
import com.yashoid.office.employee.EmployeeManager;

import java.util.HashMap;

/**
 * Created by Yashar on 4/13/2017.
 */

public class Office {

    public static class SectionDescription {

        public final String name;
        public final int employeeCount;

        public SectionDescription(String name, int employeeCount) {
            this.name = name;
            this.employeeCount = employeeCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof SectionDescription)) {
                return false;
            }

            return name.equals(((SectionDescription) obj).name);
        }

    }

    private HashMap<String, SectionManager> mSectionManagers;

    private EmployeeManager mImmediateEmployeeManager;

    private Handler mMainThreadHandler;

    public Office(Handler mainThreadHandler, int immediateEmployees, SectionDescription... sectionDescriptions) {
        mSectionManagers = new HashMap<>(sectionDescriptions.length);

        for (int i=0; i<sectionDescriptions.length; i++) {
            SectionManager sectionManager = new SectionManager(sectionDescriptions[i].name, sectionDescriptions[i].employeeCount);

            mSectionManagers.put(sectionDescriptions[i].name, sectionManager);
        }

        mImmediateEmployeeManager = new EmployeeManager("_immediate", immediateEmployees);

        mMainThreadHandler = mainThreadHandler;
    }

    public void close() {
        synchronized (this) {
            for (SectionManager sectionManager: mSectionManagers.values()) {
                sectionManager.close();
            }

            mSectionManagers.clear();

            mImmediateEmployeeManager.relieveEmployees();
        }
    }

    private boolean isClosed() {
        synchronized (this) {
            return mImmediateEmployeeManager.areEmployeesRelieved();
        }
    }

    public boolean assignTask(String sectionName, Runnable task, int priority) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            getSectionManager(sectionName).assignTask(task, priority);

            return true;
        }
    }

    public boolean assignTaskAndWait(String sectionName, Runnable task, int priority) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            getSectionManager(sectionName).assignTaskAndWait(task, priority);

            return true;
        }
    }

    public void addSection(SectionDescription sectionDescription) {
        addSection(sectionDescription.name, sectionDescription.employeeCount);
    }

    public void addSection(String name, int employeeCount) {
        if (name == null) {
            throw new IllegalArgumentException("Section name can not be null.");
        }

        if (mSectionManagers.containsKey(name)) {
            throw new SectionNameDuplicationException("Section with name '" + name + "' already exists.");
        }

        synchronized (mSectionManagers) {
            mSectionManagers.put(name, new SectionManager(name, employeeCount));
        }
    }

    public boolean performTaskImmediately(Runnable task) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            getImmediateEmployee().assignTask(task);

            return true;
        }
    }

    public boolean performTaskImmediatelyAndWait(Runnable task) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            getImmediateEmployee().assignTaskAndWait(task);

            return true;
        }
    }

    /**
     *
     * @param sectionName
     * @param task
     * @param priority
     * @return true if the task has truly canceled.
     */
    public boolean cancelTask(String sectionName, Runnable task, int priority) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            return getSectionManager(sectionName).cancelTask(task, priority);
        }
    }

    /**
     *
     * @param sectionName
     * @param task
     * @return true if the task has truly canceled.
     */
    public boolean cancelTask(String sectionName, Runnable task) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            return getSectionManager(sectionName).cancelTask(task);
        }
    }

    /**
     *
     * @param task
     * @return true if the task has truly canceled.
     */
    public boolean cancelTask(Runnable task) {
        synchronized (this) {
            if (isClosed()) {
                return false;
            }

            for (String sectionName: mSectionManagers.keySet()) {
                if (getSectionManager(sectionName).cancelTask(task)) {
                    return true;
                }
            }

            return false;
        }
    }

    public void runOnMainThread(Runnable task) {
        mMainThreadHandler.post(task);
    }

    public void runOnMainThreadAndWait(Runnable task) {
        mMainThreadHandler.post(task);

        final Object lock = new Object();

        mMainThreadHandler.post(new Runnable() {

            @Override
            public void run() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

        });

        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) { }
        }
    }

    public void cancelFromMainThread(Runnable task) {
        mMainThreadHandler.removeCallbacks(task);
    }

    private SectionManager getSectionManager(String sectionName) {
        synchronized (mSectionManagers) {
            SectionManager sectionManager = mSectionManagers.get(sectionName);

            if (sectionManager == null) {
                throw new IllegalStateException("No section found with name '" + sectionName + "'.");
            }

            return sectionManager;
        }
    }

    private Employee getImmediateEmployee() {
        Employee employee = mImmediateEmployeeManager.findFreeEmployee(false);

        if (employee != null) {
            return employee;
        }

        return mImmediateEmployeeManager.hireTemporaryEmployee();
    }

}
