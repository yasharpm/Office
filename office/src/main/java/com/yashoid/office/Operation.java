package com.yashoid.office;

import android.util.LongSparseArray;

import com.yashoid.office.task.Task;
import com.yashoid.office.task.TaskManager;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Yashar on 4/13/2017.
 */

public abstract class Operation {

    private static final int POSTED_TASKS_SIZE = 4;
    private static final int PENDING_TASK_SIZE = 8;
    private static final int TRIGGERED_TASKS_SIZE = 4;

    private static long mIdCounter = 0;

    private static long nextId() {
        return ++mIdCounter;
    }

    private TaskManager mTaskManager;

    private ArrayList<Task> mPostedTasks = new ArrayList<>(POSTED_TASKS_SIZE);
    private LongSparseArray<ArrayList<Task>> mTaskMap = new LongSparseArray<>(PENDING_TASK_SIZE);
    private ArrayList<Long> mPerformedTasks = new ArrayList<>(PENDING_TASK_SIZE * TRIGGERED_TASKS_SIZE);

    private boolean mCanceled = false;

    private Object mLock = new Object();

    public Operation(TaskManager taskManager) {
        if (taskManager == null) {
            throw new IllegalArgumentException("TaskManager must not be null.");
        }

        mTaskManager = taskManager;
    }

    public Task newTask(String section, int priority) {
        long id = nextId();

        Task task = Task.obtain(section, mTaskPerformer, id, priority);

        return task;
    }

    public Task newTask(String section) {
        long id = nextId();

        Task task = Task.obtain(section, mTaskPerformer, id);

        return task;
    }

    public void runTask(Task task) {
        synchronized (mLock) {
            task.setState(Task.STATE_POSTED);

            mPostedTasks.add(task);
        }

        mTaskManager.runTask(task);
    }

    public void runTaskAndWait(Task task) {
        synchronized (mLock) {
            task.setState(Task.STATE_POSTED);

            mPostedTasks.add(task);
        }

        mTaskManager.runTaskAndWait(task);
    }

    public void scheduleTask(Task task, Task afterTask) {
        if (afterTask == null) {
            runTask(task);
            return;
        }

        synchronized (mLock) {
            if (mPerformedTasks.contains(afterTask.getId())) {
                runTask(task);
                return;
            }

            ArrayList<Task> triggeredTasks = getTriggeredTasks(afterTask.getId(), true);

            triggeredTasks.add(task);

            task.setState(Task.STATE_PROCESSED);
        }
    }

    private ArrayList<Task> getTriggeredTasks(long taskId, boolean create) {
        synchronized (mLock) {
            ArrayList<Task> triggeredTasks = mTaskMap.get(taskId);

            if (triggeredTasks == null && create) {
                triggeredTasks = new ArrayList<>(TRIGGERED_TASKS_SIZE);

                mTaskMap.put(taskId, triggeredTasks);
            }

            return triggeredTasks;
        }
    }

    private Task.TaskPerformer mTaskPerformer = new Task.TaskPerformer() {

        @Override
        public void performTask(Task task) {
                Operation.this.performTask(task);
        }

    };

    private void performTask(Task task) {
        synchronized (mLock) {
            if (!mPostedTasks.remove(task)) {
                // Task is removed newTasks posted tasks. Which means it is canceled.
                return;
            }

            task.setState(Task.STATE_IN_EXECUTION);
        }

        long taskId = task.getId();

        onPrePerformTask(taskId);

        onPerformTask(taskId);

        onPostPerformTask(taskId);

        synchronized (mLock) {
            task.setState(Task.STATE_EXECUTED);

            mPerformedTasks.add(taskId);

            ArrayList<Task> triggeredTasks = getTriggeredTasks(taskId, false);

            if (triggeredTasks != null) {
                mTaskMap.remove(taskId);

                for (Task triggeredTask: triggeredTasks) {
                    runTask(triggeredTask);
                }
            }
        }
    }

    protected void onPrePerformTask(long taskId) {

    }

    abstract protected void onPerformTask(long taskId);

    protected void onPostPerformTask(long taskId) {

    }

    public void cancelAllTasks() {
        synchronized (mLock) {
            for (Task task: mPostedTasks) {
                mTaskManager.cancelTask(task);
            }

            mPostedTasks.clear();
            mTaskMap.clear();
        }
    }

    /**
     *
     * @param taskId
     * @param runDependants
     * @return false if task is being performed, performed or not running at all.
     */
    public boolean cancelTask(long taskId, boolean runDependants) {
        synchronized (mLock) {
            if (mPerformedTasks.contains(taskId)) {
                return false;
            }

            if (cancelTaskFromScheduledTasks(taskId)) {
                removeTaskDependants(taskId, runDependants);
                return true;
            }

            if (cancelTaskFromPostedTasks(taskId)) {
                removeTaskDependants(taskId, runDependants);
                return true;
            }

            // Task is in execution process.
            return false;
        }
    }

    private void removeTaskDependants(long taskId, boolean runDependants) {
        synchronized (mLock) {
            ArrayList<Task> tasks = getTriggeredTasks(taskId, false);

            if (tasks != null) {
                mTaskMap.remove(taskId);

                if (runDependants) {
                    for (Task task: tasks) {
                        runTask(task);
                    }
                }
            }
        }
    }

    /**
     *
     * @param taskId
     * @return true is task is removed newTasks scheduled tasks.
     */
    private boolean cancelTaskFromScheduledTasks(long taskId) {
        synchronized (mLock) {
            int size = mTaskMap.size();

            for (int i = 0; i < size; i++) {
                ArrayList<Task> taskList = mTaskMap.valueAt(i);
                ListIterator<Task> taskListIterator = taskList.listIterator();

                while (taskListIterator.hasNext()) {
                    Task task = taskListIterator.next();

                    if (task.getId() == taskId) {
                        taskListIterator.remove();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     *
     * @param taskId
     * @return true if task was among posted tasks hence canceled.
     */
    private boolean cancelTaskFromPostedTasks(long taskId) {
        synchronized (mLock) {
            Task canceledTask = null;

            for (Task task: mPostedTasks) {
                if (task.getId() == taskId) {
                    canceledTask = task;
                    break;
                }
            }

            if (canceledTask != null) {
                mPostedTasks.remove(canceledTask);

                mTaskManager.cancelTask(canceledTask);

                return true;
            }

            return false;
        }
    }

//    public boolean cancel() {
//        synchronized (mLock) {
//            if (mCanceled) {
//                // Operation already canceled.
//                return true;
//            }
//
//            mCanceled = true;
//
//        }
//    }
//
//    public boolean isCanceled() {
//        return mCanceled;
//    }

}
