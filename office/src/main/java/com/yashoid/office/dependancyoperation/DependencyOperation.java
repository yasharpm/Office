package com.yashoid.office.dependancyoperation;

import com.yashoid.office.Operation;
import com.yashoid.office.task.TaskManager;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Yashar on 9/1/2017.
 */

public class DependencyOperation extends Operation {

    private ArrayList<TaskRunCondition> mRunConditions;

    private ArrayList<TaskGroup> mRunningTaskGroups;

    private ArrayList<TaskGroup> mRanTaskGroups;

    private Object mLock = new Object();

    protected DependencyOperation(TaskManager taskManager, ArrayList<TaskRunCondition> runConditions) {
        super(taskManager);

        mRunConditions = runConditions;

        mRunningTaskGroups = new ArrayList<>(runConditions.size());

        mRanTaskGroups = new ArrayList<>(runConditions.size());
    }

    public DependencyOperation(TaskManager taskManager) {
        this(taskManager, new ArrayList<TaskRunCondition>());
    }

    public DependencyOperation start() {
        iterateForward();

        return this;
    }

    public SequenceBuilder perform(TaskGroup... taskGroups) {
        return new SequenceBuilder(taskGroups);
    }

    private void onOperationRunFinished() {
        // This is here just for convenience.
    }

    private void iterateForward() {
        synchronized (mLock) {
            ArrayList<TaskGroup> independentTasks = getIndependentTasks();

            if (independentTasks.isEmpty()) {
                synchronized (mRunningTaskGroups) {
                    if (mRunningTaskGroups.isEmpty()) {
                        onOperationRunFinished();
                    }
                }
                return;
            }

            synchronized (mRunningTaskGroups) {
                mRunningTaskGroups.addAll(independentTasks);
            }

            for (TaskGroup taskGroup: independentTasks) {
                taskGroup.execute(this);
            }
        }
    }

    @Override
    protected void onPerformTask(long taskId) {
        ArrayList<TaskGroup> taskGroups;

        synchronized (mRunningTaskGroups) {
            taskGroups = new ArrayList<>(mRunningTaskGroups);
        }

        for (TaskGroup taskGroup: taskGroups) {
            if (taskGroup.performTask(taskId)) {
                return;
            }
        }
    }

    protected void onTaskGroupPerformed(TaskGroup taskGroup) {
        synchronized (mLock) {
            mRanTaskGroups.add(taskGroup);

            synchronized (mRunningTaskGroups) {
                mRunningTaskGroups.remove(taskGroup);
            }

            synchronized (mRunConditions) {
                ListIterator<TaskRunCondition> runConditionListIterator = mRunConditions.listIterator();

                while (runConditionListIterator.hasNext()) {
                    TaskRunCondition runCondition = runConditionListIterator.next();

                    runCondition.removeTaskGroup(taskGroup);

                    if (runCondition.getEnabledTasks().size() == 0) {
                        runConditionListIterator.remove();
                    }
                }
            }

            iterateForward();
        }
    }

    private ArrayList<TaskGroup> getIndependentTasks() {
        synchronized (mRunConditions) {
            ArrayList<TaskGroup> mTaskGroups = new ArrayList<>(mRunConditions.size());

            ListIterator<TaskRunCondition> runConditionListIterator = mRunConditions.listIterator();

            while (runConditionListIterator.hasNext()) {
                TaskRunCondition runCondition = runConditionListIterator.next();

                if (runCondition.isIndependant()) {
                    runConditionListIterator.remove();

                    mTaskGroups.addAll(runCondition.getEnabledTasks());
                }
            }

            return mTaskGroups;
        }
    }

    public class SequenceBuilder {

        private TaskGroup[] mTasks;

        private SequenceBuilder(TaskGroup[] tasks) {
            mTasks = tasks;
        }

        public DependencyOperation after(TaskGroup... tasks) {
            synchronized (mLock) {
                TaskRunCondition runCondition = new TaskRunCondition(tasks, mTasks);

                for (TaskGroup taskGroup: mRanTaskGroups) {
                    runCondition.removeTaskGroup(taskGroup);
                }

                if (runCondition.isIndependant() && runCondition.getEnabledTasks().size() == 0) {
                    return DependencyOperation.this;
                }

                synchronized (mRunConditions) {
                    mRunConditions.add(new TaskRunCondition(tasks, mTasks));
                }
            }

            iterateForward();

            return DependencyOperation.this;
        }

        public DependencyOperation immediately() {
            return after();
        }

    }

}
