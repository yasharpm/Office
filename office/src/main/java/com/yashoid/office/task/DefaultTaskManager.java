package com.yashoid.office.task;

/**
 * Created by Yashar on 4/13/2017.
 */

public class DefaultTaskManager {

    public static final int DEFAULT_NETWORK_WORKERS = 5;
    public static final int DEFAULT_DB_READ_WORKERS = 3;
    public static final int DEFAULT_DB_WRITE_WORKERS = 1;
    public static final int DEFAULT_CALCULATION_WORKERS = 4;

    private static TaskManager mInstance = null;

    public static TaskManager getInstance() {
        if (mInstance == null) {
            mInstance = new TaskManagerBuilder()
                    .addNetworkSection(DEFAULT_NETWORK_WORKERS)
                    .addDatabaseReadSection(DEFAULT_DB_READ_WORKERS)
                    .addDatabaseReadWriteSection(DEFAULT_DB_WRITE_WORKERS)
                    .addCalculationSection(DEFAULT_CALCULATION_WORKERS)
                    .build();
        }

        return mInstance;
    }

    private DefaultTaskManager() {

    }

}
