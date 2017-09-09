package com.yashoid.office.dependancyoperation;

import android.util.LongSparseArray;

import com.yashoid.office.task.Task;

/**
 * Created by Yashar on 9/1/2017.
 */

public class TaskGroup {

    private boolean mIsParallel;
    private TaskDescriptor[] mTasks;

    private DependencyOperation mOperation;
    private int mCounter;

    private LongSparseArray<Integer> mIdToIndexMap;

    public TaskGroup(boolean parallel, TaskDescriptor... tasks) {
        mIsParallel = parallel;
        this.mTasks = tasks;
    }

    public TaskGroup(String section, int priority, boolean parallel, Runnable... tasks) {
        mIsParallel = parallel;
        this.mTasks = OperationBuilder.newTasks(section, priority, tasks);
    }

    public TaskGroup(String section, boolean parallel, Runnable... tasks) {
        this(section, 0, parallel, tasks);
    }

    public TaskGroup(boolean parallel, String section, int priority, Object runnerObject, String... runnerMethods) {
        this.mIsParallel = parallel;
        this.mTasks = OperationBuilder.newTasks(section, priority, runnerObject, runnerMethods);
    }

    public TaskGroup(boolean parallel, String section, Object runnerObject, String... runnerMethods) {
        this(parallel, section, 0, runnerObject, runnerMethods);
    }

    protected void execute(DependencyOperation operation) {
        if (mTasks.length == 0) {
            operation.onTaskGroupPerformed(this);
            return;
        }

        mOperation = operation;

        mIdToIndexMap = new LongSparseArray<>(mTasks.length);

        mCounter = 0;

        int index = 0;

        Task previousTask = null;

        for (TaskDescriptor taskDescriptor: mTasks) {
            Task task = operation.newTask(taskDescriptor.getSection(), taskDescriptor.getPriority());

            synchronized (mIdToIndexMap) {
                mIdToIndexMap.put(task.getId(), index);
            }

            if (mCounter == 0 || mIsParallel) {
                operation.runTask(task);
            }
            else {
                operation.scheduleTask(task, previousTask);
            }

            previousTask = task;

            index++;
        }
    }

    protected boolean performTask(long taskId) {
        Integer index;

        synchronized (mIdToIndexMap) {
            index = mIdToIndexMap.get(taskId);
        }

        if (index == null) {
            return false;
        }

        mTasks[index].getRunner().run();
        mTasks[index].clear();

        mCounter++;

        if (mCounter == mTasks.length) {
            mOperation.onTaskGroupPerformed(this);
        }

        return true;
    }

}
