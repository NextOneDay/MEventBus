package com.nextoneday.annoation;
/*
 * ===========================================================================================
 * = COPYRIGHT
 *          PAX Computer Technology(Shenzhen) CO., LTD PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or nondisclosure
 *   agreement with PAX Computer Technology(Shenzhen) CO., LTD and may not be copied or
 *   disclosed except in accordance with the terms in that agreement.
 *     Copyright (C) 2018-? PAX Computer Technology(Shenzhen) CO., LTD All rights reserved.
 * Description: // Detail description about the function of this module,
 *             // interfaces with the other modules, and dependencies.
 * Revision History:
 * Date	                 Author	                Action
 *  	                   	Create/Add/Modify/Delete
 * ===========================================================================================
 */

import java.lang.reflect.Method;

/**
 * Author: shah
 * Date : 2020/5/27.
 * Desc : SubscriberMethod
 */
public class SubscriberMethod {

    //这是定义的方法的一个封装类，封装包括方法，方法参数和粘性，优先级 ,线程模式等
    private Method mMethod;
    private Class<?> typeEvent;
    private boolean sticky;
    private int priority;
    private ThreadMode mThreadMode;
    private String mMethodName;

    public SubscriberMethod( Class<?> subscriberClass, String methodName,Class<?> typeEvent, boolean sticky, int priority, ThreadMode threadMode) {

        this.typeEvent = typeEvent;
        this.sticky = sticky;
        this.priority = priority;
        mThreadMode = threadMode;
        mMethodName = methodName;

        try {
          mMethod =  subscriberClass.getDeclaredMethod(methodName, typeEvent);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public Method getMethod() {
        return mMethod;
    }

    public void setMethod(Method method) {
        mMethod = method;
    }

    public Class<?> getTypeEvent() {
        return typeEvent;
    }

    public void setTypeEvent(Class<?> typeEvent) {
        this.typeEvent = typeEvent;
    }

    public boolean getSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ThreadMode getThreadMode() {
        return mThreadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        mThreadMode = threadMode;
    }

    public String getMethodName() {
        return mMethodName;
    }

    public void setMethodName(String methodName) {
        mMethodName = methodName;
    }

    @Override
    public String toString() {
        return "SubscriberMethod{" +
                "mMethod=" + mMethod +
                ", typeEvent=" + typeEvent +
                ", sticky=" + sticky +
                ", priority=" + priority +
                ", mThreadMode=" + mThreadMode +
                ", mMethodName='" + mMethodName + '\'' +
                '}';
    }
}
