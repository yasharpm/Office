package com.yashoid.office.employee;

import java.util.ArrayList;

/**
 * Created by Yashar on 9/20/2016.
 */
public class EmployeeManager {

    public interface OnAllEmployeesAreFreeListener {

        void onAllEmployeesAreFree();

    }

    public interface OnFreeEmployeeAvailableListener {

        void onFreeEmployeeAvailable();

    }

    private String mName;
    private int mTemporaryEmployeeCounter = 0;

    private volatile ArrayList<Employee> mFreeEmployees;
    private volatile ArrayList<Employee> mBusyEmployees;
    private volatile ArrayList<Employee> mReservedEmployees;

    private boolean mEmployeesAreRelieved = false;

    private Object mFreeEmployeeAvailableFlag = new Object();
    private Object mAllEmployeesAreFreeFlag = new Object();

    private ArrayList<OnAllEmployeesAreFreeListener> mOnAllEmployeesAreFreeListeners = new ArrayList<>(2);
    private ArrayList<OnFreeEmployeeAvailableListener> mOnFreeEmployeeAvailableListeners = new ArrayList<>(2);

    public EmployeeManager(String name, int count) {
        mFreeEmployees = new ArrayList<>(count);
        mBusyEmployees = new ArrayList<>(count);
        mReservedEmployees = new ArrayList<>(count);

        if (name == null) {
            mName = "EM-" + hashCode() + "-";
        }
        else {
            mName = name.concat("-");
        }

        for (int i=0; i<count; i++) {
            String employeeName = mName.concat("" + i);

            mFreeEmployees.add(new Employee(this, employeeName));
        }
    }

    public ArrayList<Employee> getAllEmployees() {
        synchronized (this) {
            ArrayList<Employee> employees = new ArrayList<>(mFreeEmployees.size() + mReservedEmployees.size());

            employees.addAll(mFreeEmployees);
            employees.addAll(mReservedEmployees);

            return employees;
        }
    }

    public Employee reserveFreeEmployee(boolean waitIfAllAreReserved) {
        if (mEmployeesAreRelieved) {
            return null;
        }

        synchronized (this) {
            if (mFreeEmployees.size() > 0) {
                Employee freeEmployee = mFreeEmployees.get(0);

                mFreeEmployees.remove(freeEmployee);
                mReservedEmployees.add(freeEmployee);

                return freeEmployee;
            }
        }

        if (waitIfAllAreReserved) {
            synchronized (mFreeEmployeeAvailableFlag) {
                try {
                    mFreeEmployeeAvailableFlag.wait();
                } catch (InterruptedException e) { }

                return reserveFreeEmployee(true);
            }
        }

        return null;
    }

    public Employee findFreeEmployee(boolean waitIfNoFreeEmployees) {
        if (mEmployeesAreRelieved) {
            return null;
        }

        synchronized (this) {
            if (mFreeEmployees.size() > 0) {
                return mFreeEmployees.get(0);
            }
        }

        if (waitIfNoFreeEmployees) {
            synchronized (mFreeEmployeeAvailableFlag) {
                try {
                    mFreeEmployeeAvailableFlag.wait();
                } catch (InterruptedException e) { }

                return findFreeEmployee(true);
            }
        }

        return null;
    }

    public void unreserveEmployee(Employee employee) {
        if (mEmployeesAreRelieved) {
            return;
        }

        if (!mReservedEmployees.contains(employee)) {
            return;
        }

        synchronized (this) {
            mReservedEmployees.remove(employee);
            mFreeEmployees.add(employee);

            synchronized (mFreeEmployeeAvailableFlag) {
                mFreeEmployeeAvailableFlag.notifyAll();
            }
        }
    }

    public Employee hireTemporaryEmployee() {
        if (mEmployeesAreRelieved) {
            return null;
        }

        return new Employee(this, mName.concat("temp-" + (mTemporaryEmployeeCounter)));
    }

    public boolean areAllEmployeesFree() {
        synchronized (this) {
            return mReservedEmployees.size() == 0;
        }
    }

    public void notifyWhenAllEmployeesAreFree(OnAllEmployeesAreFreeListener listener) {
        synchronized (this) {
            if (mReservedEmployees.size() == 0) {
                listener.onAllEmployeesAreFree();
            }
            else {
                mOnAllEmployeesAreFreeListeners.add(listener);
            }
        }
    }

    public void notifyWhenFreeEmployeeIsAvailable(OnFreeEmployeeAvailableListener listener) {
        synchronized (this) {
            if (mFreeEmployees.size() > 0) {
                listener.onFreeEmployeeAvailable();
            }
            else {
                mOnFreeEmployeeAvailableListeners.add(listener);
            }
        }
    }

    public void waitUntilAllEmployeesAreFree() {
        if (areAllEmployeesFree()) {
            return;
        }

        synchronized (mAllEmployeesAreFreeFlag) {
            try {
                mAllEmployeesAreFreeFlag.wait();
            } catch (InterruptedException e) { }
        }
    }

    public void relieveEmployees() {
        synchronized (this) {
            if (mEmployeesAreRelieved) {
                return;
            }

            mEmployeesAreRelieved = true;

            for (Employee employee: mFreeEmployees) {
                employee.relieve();
            }

            for (Employee employee: mReservedEmployees) {
                employee.relieve();
            }

            mFreeEmployees.clear();
        }
    }

    public boolean areEmployeesRelieved() {
        return mEmployeesAreRelieved;
    }

    protected void onEmployeeIsBusy(Employee employee) {
        synchronized (this) {
            if (!isEmployeeTemporary(employee)) {
                if (mFreeEmployees.contains(employee)) {
                    mFreeEmployees.remove(employee);
                }

                if (!mBusyEmployees.contains(employee)) {
                    mBusyEmployees.add(employee);
                }
            }
        }
    }

    private boolean isEmployeeTemporary(Employee employee) {
        synchronized (this) {
            return !mFreeEmployees.contains(employee) && !mReservedEmployees.contains(employee) && !mBusyEmployees.contains(employee);
        }
    }

    protected void onEmployeeIsFree(Employee employee) {
        synchronized (this) {
            if (!isEmployeeTemporary(employee)) {
                if (mEmployeesAreRelieved) {
                    mBusyEmployees.remove(employee);

                    employee.relieve();

                    return;
                }

                mFreeEmployees.add(employee);
                mBusyEmployees.remove(employee);

                synchronized (mFreeEmployeeAvailableFlag) {
                    mFreeEmployeeAvailableFlag.notify();
                }

                synchronized (this) {
                    while (mOnFreeEmployeeAvailableListeners.size() > 0 && mFreeEmployees.size() > 0) {
                        OnFreeEmployeeAvailableListener listener = mOnFreeEmployeeAvailableListeners.get(0);

                        mOnFreeEmployeeAvailableListeners.remove(listener);

                        listener.onFreeEmployeeAvailable();
                    }
                }

                if (mBusyEmployees.size() == 0) {
                    synchronized (mAllEmployeesAreFreeFlag) {
                        mAllEmployeesAreFreeFlag.notifyAll();
                    }
                }

                while (mBusyEmployees.size() == 0 && mOnAllEmployeesAreFreeListeners.size() > 0) {
                    OnAllEmployeesAreFreeListener listener = mOnAllEmployeesAreFreeListeners.get(0);

                    mOnAllEmployeesAreFreeListeners.remove(listener);

                    listener.onAllEmployeesAreFree();
                }
            }
            else {
                employee.relieve();
            }
        }
    }

}
