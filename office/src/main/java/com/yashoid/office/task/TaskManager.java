package com.yashoid.office.task;

import android.os.Handler;

import com.yashoid.office.office.Office;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by Yashar on 4/13/2017.
 */

public class TaskManager {

    public static final String MAIN = "main";
    public static final String NETWORK = "network";
    public static final String DATABASE_READ = "data_read";
    public static final String DATABASE_READWRITE = "data_readwrite";
    public static final String CALCULATION = "calculation";

    private Office mOffice;

    private ArrayList<Office.SectionDescription> mSectionDescriptions;

    public TaskManager(Office.SectionDescription... sectionDescriptions) {
        mOffice = new Office(new Handler(), 0, sectionDescriptions);

        mSectionDescriptions = new ArrayList<>(sectionDescriptions.length);
        Collections.addAll(mSectionDescriptions, sectionDescriptions);
    }

    public void addSection(String name, int workerCount) {
        addSection(new Office.SectionDescription(name, workerCount));
    }

    public void addSection(Office.SectionDescription sectionDescription) {
        synchronized (mSectionDescriptions) {
            mOffice.addSection(sectionDescription);

            mSectionDescriptions.add(sectionDescription);
        }
    }

    public List<Office.SectionDescription> getSectionDescriptions() {
        synchronized (mSectionDescriptions) {
            return Collections.unmodifiableList(mSectionDescriptions);
        }
    }

    public ExecutorService getExecutor(String section, int priority) {
        return new TaskExecutor(this, section, priority);
    }

    public ExecutorService getExecutor(String section) {
        return getExecutor(section, 0);
    }

    public void runTask(String section, Runnable task, int priority) {
        if (MAIN.equals(section)) {
            mOffice.runOnMainThread(task);
            return;
        }

        mOffice.assignTask(section, task, priority);
    }

    public void runTaskAndWait(String section, Runnable task, int priority) {
        if (MAIN.equals(section)) {
            mOffice.runOnMainThreadAndWait(task);
            return;
        }

        mOffice.assignTaskAndWait(section, task, priority);
    }

    /**
     *
     * @param section
     * @param task
     * @param priority
     * @return true if task has been canceled.
     */
    public boolean cancelTask(String section, Runnable task, int priority) {
        if (MAIN.equals(section)) {
            mOffice.cancelFromMainThread(task);
            return true;
        }

        return mOffice.cancelTask(section, task, priority);
    }

    public void runTaskImmediately(Runnable task) {
        mOffice.performTaskImmediately(task);
    }

    public void runTaskImmediatelyAndWait(Runnable task) {
        mOffice.performTaskImmediatelyAndWait(task);
    }

    public void runTask(Task task) {
        String section = task.getSection();

        if (section == null) {
            throw new IllegalStateException("Section is not set on task.");
        }

        runTask(section, task, task.getPriority());
    }

    public void runTaskAndWait(Task task) {
        String section = task.getSection();

        if (section == null) {
            throw new IllegalStateException("Section is not set on task.");
        }

        runTaskAndWait(section, task, task.getPriority());
    }

    /**
     *
     * @param task
     * @return true is task is known to have been canceled.
     */
    public boolean cancelTask(Task task) {
        String section = task.getSection();

        if (section == null) {
            throw new IllegalStateException("Section is not set on task.");
        }

        return cancelTask(section, task, task.getPriority());
    }

}
