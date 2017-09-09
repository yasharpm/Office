package com.yashoid.office.dependancyoperation;

/**
 * Created by Yashar on 9/1/2017.
 */

public class TaskDescriptor {

    private String mSection;
    private int mPriority;
    private Runnable mRunner;

    public TaskDescriptor(String section, int priority, Runnable runner) {
        this.mSection = section;
        this.mPriority = priority;
        this.mRunner = runner;
    }

    public TaskDescriptor(String section, Runnable runner) {
        this(section, 0, runner);
    }

    public TaskDescriptor(String section, int priority, final Object runnerObject, final String runnerMethod) {
        this(section, priority, new ObjectMethodRunner(runnerObject, runnerMethod));
    }

    public TaskDescriptor(String section, Object runnerObject, String runnerMethod) {
        this(section, 0, runnerObject, runnerMethod);
    }

    public String getSection() {
        return mSection;
    }

    public int getPriority() {
        return mPriority;
    }

    public Runnable getRunner() {
        return mRunner;
    }

    protected void clear() {
        mRunner = null;
    }

}
