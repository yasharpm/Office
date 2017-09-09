package com.yashoid.office.dependancyoperation;

import com.yashoid.office.task.TaskManager;

import java.util.ArrayList;

/**
 * Created by Yashar on 9/1/2017.
 */

public class OperationBuilder {

    public static TaskDescriptor[] newTasks(String section, int priority, Runnable... tasks) {
        TaskDescriptor[] descriptors = new TaskDescriptor[tasks.length];

        for (int i = 0; i < tasks.length; i++) {
            descriptors[i] = new TaskDescriptor(section, priority, tasks[i]);
        }

        return descriptors;
    }

    public static TaskDescriptor[] newTasks(String section, Runnable... tasks) {
        return newTasks(section, 0, tasks);
    }

    public static TaskDescriptor[] newTasks(String section, int priority, Object runnerObject, String[] runnerMethods) {
        TaskDescriptor[] descriptors = new TaskDescriptor[runnerMethods.length];

        for (int i = 0; i < runnerMethods.length; i++) {
            descriptors[i] = new TaskDescriptor(section, priority, runnerObject, runnerMethods[i]);
        }

        return descriptors;
    }

    public static TaskDescriptor[] newTasks(String section, Object runnerObject, String[] runnerMethods) {
        return newTasks(section, 0, runnerObject, runnerMethods);
    }

    public static TaskGroup singleTask(TaskDescriptor task) {
        return new TaskGroup(false, task);
    }

    public static TaskGroup singleTask(String section, int priority, Runnable task) {
        return new TaskGroup(section, priority, false, task);
    }

    public static TaskGroup singleTask(String section, Runnable task) {
        return new TaskGroup(section, false, task);
    }

    public static TaskGroup singleTask(String section, int priority, Object runnerObject, String runnerMethod) {
        return new TaskGroup(false, section, priority, runnerObject, runnerMethod);
    }

    public static TaskGroup singleTask(String section, Object runnerObject, String runnerMethod) {
        return new TaskGroup(false, section, runnerObject, runnerMethod);
    }

    public static TaskGroup sequentialTasks(TaskDescriptor... tasks) {
        return new TaskGroup(false, tasks);
    }

    public static TaskGroup sequentialTasks(String section, int priority, Runnable... tasks) {
        return new TaskGroup(section, priority, false, tasks);
    }

    public static TaskGroup sequentialTasks(String section, Runnable... tasks) {
        return new TaskGroup(section, false, tasks);
    }

    public static TaskGroup sequentialTasks(String section, int priority, Object runnerObject, String... runnerMethods) {
        return new TaskGroup(false, section, priority, runnerObject, runnerMethods);
    }

    public static TaskGroup sequentialTasks(String section, Object runnerObject, String... runnerMethods) {
        return new TaskGroup(false, section, runnerObject, runnerMethods);
    }

    public static TaskGroup parallelTasks(TaskDescriptor... tasks) {
        return new TaskGroup(true, tasks);
    }

    public static TaskGroup parallelTasks(String section, int priority, Runnable... tasks) {
        return new TaskGroup(section, priority, true, tasks);
    }

    public static TaskGroup parallelTasks(String section, Runnable... tasks) {
        return new TaskGroup(section, true, tasks);
    }

    public static TaskGroup parallelTasks(String section, int priority, Object runnerObject, String... runnerMethods) {
        return new TaskGroup(true, section, priority, runnerObject, runnerMethods);
    }

    public static TaskGroup parallelTasks(String section, Object runnerObject, String... runnerMethods) {
        return new TaskGroup(true, section, runnerObject, runnerMethods);
    }

    private ArrayList<TaskRunCondition> mRunConditions = new ArrayList<>();

    public OperationBuilder() {

    }

    public SequenceBuilder perform(TaskGroup... tasks) {
        return new SequenceBuilder(tasks);
    }

    public DependencyOperation build(TaskManager taskManager) {
        return new DependencyOperation(taskManager, mRunConditions);
    }

    public class SequenceBuilder {

        private TaskGroup[] mTasks;

        private SequenceBuilder(TaskGroup[] tasks) {
            mTasks = tasks;
        }

        public OperationBuilder after(TaskGroup... tasks) {
            mRunConditions.add(new TaskRunCondition(tasks, mTasks));

            return OperationBuilder.this;
        }

        public OperationBuilder immediately() {
            return after();
        }

    }

}
