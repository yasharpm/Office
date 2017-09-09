package com.yashoid.office.sample;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Yashar on 9/5/2017.
 */

public class ProgressView extends View {

    private Paint mPaint;

    private float mProgress;

    public ProgressView(Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    public void setProgress(float progress) {
        mProgress = progress;

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth() * mProgress;

        canvas.drawRect(0, 0, width, getHeight(), mPaint);
    }

}
