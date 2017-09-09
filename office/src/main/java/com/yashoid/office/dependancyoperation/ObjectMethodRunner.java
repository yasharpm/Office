package com.yashoid.office.dependancyoperation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Yashar on 9/1/2017.
 */

class ObjectMethodRunner implements Runnable {

    private Object mObject;
    private Method mMethod;

    protected ObjectMethodRunner(Object object, String methodName) {
        mObject = object;

        try {
            mMethod = object.getClass().getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method '" + methodName + "' is not defined on " + object.getClass().getName() + ".", e);
        }
    }

    @Override
    public void run() {
        try {
            mMethod.invoke(mObject);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to call '" + mMethod.getName() + "' on " + mObject.getClass().getName() + ".", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to call '" + mMethod.getName() + "' on " + mObject.getClass().getName() + ".", e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ObjectMethodRunner)) {
            return false;
        }

        ObjectMethodRunner runner = (ObjectMethodRunner) obj;

        return runner.mObject.equals(mObject) && runner.mMethod.equals(mMethod);
    }

}
