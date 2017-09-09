package com.yashoid.office.task;

import com.yashoid.office.office.Office;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yashar on 9/1/2017.
 */

public class TaskManagerBuilder {

    private List<Office.SectionDescription> mSectionDescriptions = new ArrayList<>(8);

    public TaskManagerBuilder() {

    }

    public TaskManagerBuilder addSection(String name, int workerCount) {
        if (name == null) {
            throw new IllegalArgumentException("Section name can not be null.");
        }

        if (TaskManager.MAIN.equals(name)) {
            throw new IllegalArgumentException("Section name is not allowed to be '" + TaskManager.MAIN + "'");
        }

        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be greater than zero.");
        }

        Office.SectionDescription sectionDescription = new Office.SectionDescription(name, workerCount);

        if (mSectionDescriptions.contains(sectionDescription)) {
            throw new IllegalArgumentException("Section '" + name + "' already defined.");
        }

        mSectionDescriptions.add(sectionDescription);

        return this;
    }

    public TaskManagerBuilder addNetworkSection(int workerCount) {
        return addSection(TaskManager.NETWORK, workerCount);
    }

    public TaskManagerBuilder addDatabaseReadSection(int workerCount) {
        return addSection(TaskManager.DATABASE_READ, workerCount);
    }

    public TaskManagerBuilder addDatabaseReadWriteSection(int workerCount) {
        return addSection(TaskManager.DATABASE_READWRITE, workerCount);
    }

    public TaskManagerBuilder addCalculationSection(int workerCount) {
        return addSection(TaskManager.CALCULATION, workerCount);
    }

    public TaskManager build() {
        Office.SectionDescription[] sectionDescriptions =
                mSectionDescriptions.toArray(new Office.SectionDescription[mSectionDescriptions.size()]);

        return new TaskManager(sectionDescriptions);
    }

}
