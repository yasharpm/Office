package com.yashoid.office.util;

import android.support.v4.util.Pools;

import java.util.ArrayList;

/**
 * Created by Yashar on 4/13/2017.
 */

public abstract class FlexiblePool<T> implements Pools.Pool<T> {

    private static final int MAXIMUM_POOL_SIZE = 100;

    private ArrayList<T> mInstances;

    public FlexiblePool() {
        mInstances = new ArrayList<>();
    }

    @Override
    public T acquire() {
        synchronized (this) {
            if (mInstances.size() > 0) {
                T instance = mInstances.get(0);

                mInstances.remove(instance);

                return instance;
            }

            return newInstance();
        }
    }

    abstract protected T newInstance();

    @Override
    public boolean release(T instance) {
        synchronized (this) {
            if (mInstances.contains(instance)) {
                return true;
            }

            onInstanceReleased(instance);

            if (mInstances.size() >= MAXIMUM_POOL_SIZE) {
                return false;
            }

            mInstances.add(instance);

            return true;
        }
    }

    abstract protected void onInstanceReleased(T instance);

}
