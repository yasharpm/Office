package com.yashoid.office.dependancyoperation;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Yashar on 9/1/2017.
 */

class TaskRunCondition {

    private ArrayList<TaskGroup> mConditions;
    private ArrayList<TaskGroup> mEnabledTasks;

    protected TaskRunCondition(TaskGroup[] conditions, TaskGroup[] enabledTasks) {
        mConditions = new ArrayList<>(conditions.length);
        Collections.addAll(mConditions, conditions);

        mEnabledTasks = new ArrayList<>(enabledTasks.length);
        Collections.addAll(mEnabledTasks, enabledTasks);
    }

    protected boolean isIndependant() {
        return mConditions.isEmpty();
    }

    protected ArrayList<TaskGroup> getEnabledTasks() {
        return mEnabledTasks;
    }

    protected void removeTaskGroup(TaskGroup taskGroup) {
        mConditions.remove(taskGroup);
        mEnabledTasks.remove(taskGroup);
    }

}
