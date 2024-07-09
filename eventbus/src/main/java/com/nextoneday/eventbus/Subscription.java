package com.nextoneday.eventbus;
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

import com.nextoneday.annoation.SubscriberMethod;

/**
 * Author: shah
 * Date : 2020/5/30.
 * Desc : Subscription
 */
class Subscription {

    private  Object mSubscript;
    private SubscriberMethod mSubscriberMethod;

    public Subscription(Object subscript, SubscriberMethod subscriberMethod) {
        mSubscript = subscript;
        mSubscriberMethod = subscriberMethod;
    }

    public Object getSubscript() {
        return mSubscript;
    }

    public void setSubscript(Object subscript) {
        mSubscript = subscript;
    }

    public SubscriberMethod getSubscriberMethod() {
        return mSubscriberMethod;
    }

    public void setSubscriberMethod(SubscriberMethod subscriberMethod) {
        mSubscriberMethod = subscriberMethod;
    }
    @Override
    public boolean equals(Object other) {
        // 必须重写方法，检测激活粘性事件重复调用（同一对象注册多个）
        if (other instanceof Subscription) {
            Subscription otherSubscription = (Subscription) other;
            // 删除官方：subscriber == otherSubscription.subscriber判断条件
            // 原因：粘性事件Bug，多次调用和移除时重现，参考Subscription.java 37行
            return mSubscriberMethod.equals(otherSubscription.mSubscriberMethod);
        } else {
            return false;
        }
    }
    @Override
    public String toString() {
        return "Subscription{" +
                "mSubscript=" + mSubscript +
                ", mSubscriberMethod=" + mSubscriberMethod +
                '}';
    }
}
