
package com.yashoid.office.sample;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.yashoid.office.dependancyoperation.DependencyOperation;
import com.yashoid.office.dependancyoperation.OperationBuilder;
import com.yashoid.office.dependancyoperation.TaskGroup;
import com.yashoid.office.task.TaskManager;
import com.yashoid.office.task.TaskManagerBuilder;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final long PROGRESS_MIN_DURATION = 2000;
    private static final long PROGRESS_MAX_DURATION = 6000;

    private static final int TASK_COUNT = 9;

    private static final int[] COLORS = { 0xffff9999, 0xff99ff99, 0xff9999ff, 0xffeeee77 };

    private ViewGroup mTasks;
    private ArrayList<TaskView> mTaskViews;

    private TaskManager mTaskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTasks = (ViewGroup) findViewById(R.id.tasks);

        mTaskViews = new ArrayList<>();

        for (int i = 0; i < TASK_COUNT; i++) {
            mTaskViews.add(new TaskView(mTasks));
        }

        mTaskManager = new TaskManagerBuilder()
                .addSection("0", 3)
                .addSection("1", 3)
                .addSection("2", 3)
                .addSection("3", 3)
                .build();
    }

    public void startDemo(View v) {
        OperationBuilder builder = new OperationBuilder();

        TaskGroup zero = getGroup(0, "0");
        TaskGroup one = getGroup(1, "1");
        TaskGroup two = getGroup(2, "2");
        TaskGroup three = getGroup(3, "3");

        builder
                .perform(zero).immediately()
                .perform(one).after(zero)
                .perform(two).after(one)
                .perform(three).after(two);

        DependencyOperation operation = builder.build(mTaskManager);

        operation.start();
    }

    private TaskGroup getGroup(int index, String section) {
        ArrayList<TaskView> tasks = new ArrayList<>();

        for (TaskView taskView: mTaskViews) {
            if (taskView.mIndex == index) {
                tasks.add(taskView);
            }
        }

        TaskView[] taskArray = tasks.toArray(new TaskView[0]);

        return OperationBuilder.parallelTasks(section, taskArray);
    }

    private class TaskView implements View.OnClickListener, Runnable {

        private ImageButton mButton;
        private ProgressView mProgress;

        private int mIndex = 0;

        public TaskView(ViewGroup parent) {
            getLayoutInflater().inflate(R.layout.task, parent, true);

            View holder = parent.getChildAt(parent.getChildCount() - 1);

            mButton = (ImageButton) holder.findViewById(R.id.button);
            mProgress = (ProgressView) holder.findViewById(R.id.progress);

            mButton.setOnClickListener(this);

            update();
        }

        @Override
        public void onClick(View v) {
            mIndex = (mIndex + 1) % COLORS.length;

            mProgress.setProgress(0);

            update();
        }

        private void update() {
            mButton.setImageDrawable(new ColorDrawable(COLORS[mIndex]));
            mProgress.setColor(COLORS[mIndex]);
        }

        @Override
        public void run() {
            mProgress.setProgress(0);

            long duration = (long) (new Random().nextFloat() * (PROGRESS_MAX_DURATION - PROGRESS_MIN_DURATION) + PROGRESS_MIN_DURATION);

            long passedTime = 0;

            while (passedTime < duration) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) { }

                passedTime += 20;

                mProgress.setProgress((float) passedTime / (float) duration);
            }
        }

    }

}
