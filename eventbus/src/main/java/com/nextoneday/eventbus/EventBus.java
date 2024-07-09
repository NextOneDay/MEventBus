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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nextoneday.annoation.Subscribe;
import com.nextoneday.annoation.SubscriberInfo;
import com.nextoneday.annoation.SubscriberInfoIndex;
import com.nextoneday.annoation.SubscriberMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: shah
 * Date : 2020/5/23.
 * Desc : EventBus
 */
public final class EventBus {
    private final ExecutorService mThreadPool;
    private Handler mHandler;
    // 用来区分是apt 还是反射
   private boolean ignoreGeneratedIndex =true;

    // 方法缓存：key：订阅者MainActivity.class，value：订阅方法集合
    private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    // 订阅者类型集合，比如：订阅者MainActivity订阅了哪些EventBean，或者解除订阅的缓存。
    // key：订阅者MainActivity.class，value：EventBean集合
    private Map<Object, List<Class<?>>> typesBySubscriber;
    // EventBean缓存，key：UserInfo.class，value：订阅者（可以是多个Activity）中所有订阅的方法集合
    private Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;

    // 粘性事件缓存，key：UserInfo.class，value：UserInfo
    private final Map<Class<?>, Object> stickyEvents;
    private EventBus() {

        //post 的时候，subscriptionsByEventType 这个事件，所有的订阅者，然后直接一个循环就搞定，
        typesBySubscriber = new HashMap<>();
        subscriptionsByEventType = new HashMap<>();
        stickyEvents = new HashMap<>();


        mThreadPool = Executors.newCachedThreadPool();

        mHandler = new Handler(Looper.getMainLooper());
    }

    private static EventBus mEventBus;

    public static EventBus getDefault() {
        if (mEventBus == null) {
            synchronized (EventBus.class) {
                if (mEventBus == null) {

                    mEventBus = new EventBus();
                }
            }
        }
        return mEventBus;
    }

    // 索引接口
    private List<SubscriberInfoIndex> subscriberInfoIndexes;

    public void addIndex(SubscriberInfoIndex index) {
        if (subscriberInfoIndexes == null) {
            subscriberInfoIndexes = new ArrayList<>();
        }
        subscriberInfoIndexes.add(index);
        ignoreGeneratedIndex=false;
    }

    // 是否已经注册 / 订阅，参考EventBus.java 217行
    public synchronized boolean isRegistered(Object subscriber) {
        return typesBySubscriber.containsKey(subscriber);
    }


    /**
     * 通过注册的类查找到订阅的方法
     * @param subscriber
     */

    public void register(Object subscriber) {
        if (subscriber == null) return;

        Class<?> subscriberClass = subscriber.getClass();

        List<SubscriberMethod> annoationMethod = findSubscriberMethods(subscriberClass);

        // 同步锁，防止并发
        synchronized (this){
            for (SubscriberMethod subscriberMethod : annoationMethod) {

                subscriber(subscriber,subscriberMethod);
            }
        }
    }

    //重点中的重点，将拿到的所有方法开始订阅起来，包括粘性事件等等
    // 这里用到了好几个map集合存储
    private void subscriber(Object subscriber, SubscriberMethod subscriberMethod) {
//        1、首先将 对象和 方法进行封装起来
        Subscription  subscription = new Subscription(subscriber,subscriberMethod);

//        2. 拿到订阅的方法 的class类型
        Class<?> typeEvent = subscriberMethod.getTypeEvent();

        // 这里存储的是 事件对应的 订阅者以及订阅方法，即 XXEvent 在多个类中注册了，或者在同个类注册了多次，都会存在这里
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(typeEvent);
        if (subscriptions==null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(typeEvent, subscriptions);
        }else {
            if(subscriptions.contains(subscription)){
                Log.e("EventBus >>> ", subscriber.getClass() + "重复注册粘性事件！");
                // 执行多次粘性事件，但不添加到集合，避免订阅方法多次执行
                sticky(subscriberMethod, typeEvent, subscription);
                return;
            }
        }

        //添加到map 里面，按照优先级
        int size = subscriptions.size();
        for (int x = 0; x <= size; x++) {
            // 如果为第一次，或者是当前的订阅方法，比索引位置的订阅方法优先，插个队
            if(x == size || subscriberMethod.getPriority()> subscriptions.get(x).getSubscriberMethod().getPriority()){
                if(!subscriptions.contains(subscription)){
                    subscriptions.add(x, subscription);
                    break;
                }
            }
        }

        //订阅者的集合，
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if(subscribedEvents==null){
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }

        subscribedEvents.add(typeEvent);


        sticky(subscriberMethod,typeEvent,subscription);
    }

    // 粘性事件，注册的时候就立即执行，所以当发送post 的时候需要进行存储起来
    private void sticky(SubscriberMethod subscriberMethod, Class<?> typeEvent, Subscription subscription) {

        if(subscriberMethod.getSticky()){
            Object stickyEvent = stickyEvents.get(typeEvent);
            if (stickyEvent != null) postToSubscription(subscription, stickyEvent);

        }
    }




    /**
     * 这里要区分一下是使用apt 还是使用反射的方式
     * @param subscriberClass
     * @return
     */
    private List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> list = METHOD_CACHE.get(subscriberClass);
        if(list!=null) return list;
        if(ignoreGeneratedIndex){
            //这是反射
            list =  findUsingReflection(subscriberClass);
        }else {
            list=  findUsingInfo(subscriberClass);
        }
        if(list==null){
            throw new RuntimeException("没有数据搞毛线啊");
        }else {
            METHOD_CACHE.put(subscriberClass, list);
        }
        return list;
    }


    //从反射中找出方法集合
    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass){


         List<SubscriberMethod>  list = new ArrayList<>();

            Method[] methods = subscriberClass.getMethods();
            for (Method method : methods) {
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                if (annotation == null) continue;

                if (!method.getGenericReturnType().toString().equals("void")) {
                    throw new RuntimeException(method.getName() + " 必须是void");
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new RuntimeException(method.getName() + " 参数为一个");
                }

                SubscriberMethod bean = new SubscriberMethod(subscriberClass,
                        method.getName(),parameterTypes[0], annotation.sticky(),  annotation.priority(),annotation.threadMode());

                list.add(bean);
            }



        return list;

    }
    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass){
        SubscriberInfo subscriberInfo = getSubscriberInfo(subscriberClass);
        SubscriberMethod[] subscriberMethods = subscriberInfo.getSubscriberMethods();
        return  Arrays.asList(subscriberMethods);

    }

    private SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
        if (subscriberInfoIndexes!=null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                SubscriberInfo subscriberInfo = index.getSubscriberInfo(subscriberClass);
                if(subscriberInfo!=null){
                    return subscriberInfo;
                }
            }
        }
        return null;
    }

    public void unRegister(Object subscriber) {

        if (subscriber != null) {

            List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);


            if(subscribedTypes!=null ){
                for (Class<?> eventType : subscribedTypes) {
                    unsubscribeByEventType(subscriber,eventType );
                }
                subscribedTypes.clear();
                typesBySubscriber.remove(subscribedTypes);
            }
        }
    }

    //当解绑的时候，从map 中清楚，当前订阅者的对应的数据
//    比如mainactivity 解绑，去掉当前页面的2个event事件
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {

        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if(subscriptions!=null ){
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.getSubscript() == subscriber) {
//                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }

    }


    // 这种方法非常笨重，如果10个activity，然后每个activity都有三个event，这个时候，每发送一个事件，都要遍历完所有的数据，对性能不好

    @Deprecated
    public void post1(Object event) {

        try {

            for (Map.Entry<Class<?>, List<SubscriberMethod>> listEntry : METHOD_CACHE.entrySet()) {

                for (SubscriberMethod method : listEntry.getValue()) {

                    if (event.getClass().isAssignableFrom(method.getTypeEvent())) {
                        Subscription subscription = new Subscription(listEntry.getKey(), method);
                        postToSubscription(subscription,event);

                    }

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void post(Object event){

        postSingleEventForEventType(event,event.getClass());
    }

    private void postSingleEventForEventType(Object event, Class<?> eventType) {

        CopyOnWriteArrayList<Subscription> subscriptions ;

        synchronized (this){
            subscriptions = subscriptionsByEventType.get(eventType);
        }
        if(subscriptions!=null && !subscriptions.isEmpty()){
            for (Subscription subscription : subscriptions) {
                postToSubscription(subscription,event);
            }
        }

    }

    private void postToSubscription(final Subscription subscription, final Object event) {

        final SubscriberMethod subscriberMethod = subscription.getSubscriberMethod();
        switch (subscriberMethod.getThreadMode()) {
            case POSTING:
                invokeSubscriber(subscription, event);
                break;
            case MAIN:
                if(Looper.myLooper()==Looper.getMainLooper()){
                    invokeSubscriber(subscription,event);

                }else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription,event);
                        }
                    });
                }
                break;
            case BACKGRAND:

                if(Looper.myLooper() == Looper.getMainLooper()){

                    mThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription,event);
                        }
                    });
                }else {
                    invokeSubscriber(subscription,event);

                }
                break;

        }
    }

    private void  invokeSubscriber(Subscription subscription, Object event){
        try {
            subscription.getSubscriberMethod().getMethod().invoke(subscription.getSubscript(), event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void postSticky(Object event){
        synchronized (stickyEvents){
            stickyEvents.put(event.getClass(), event);
        }
    }


    public <T> T getStickyEvent(Class<T> eventType){
        synchronized (stickyEvents){
            return eventType.cast(stickyEvents.get(eventType));
        }
    }
    public <T>T removeStickyEvent(Class<T> eventType){
        synchronized (stickyEvents){
            return eventType.cast(stickyEvents.remove(eventType));
        }
    }

    public void removeAllStickyEvents(){
        synchronized (stickyEvents){

            stickyEvents.clear();
        }
    }

    public static  void clearCaches(){
        METHOD_CACHE.clear();
    }
}
