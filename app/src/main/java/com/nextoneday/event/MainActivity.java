package com.nextoneday.event;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.nextoneday.annoation.Subscribe;
import com.nextoneday.event.event.PostEvent;
import com.nextoneday.event.event.TestEvent;
import com.nextoneday.eventbus.EventBus;
import com.nextoneday.eventbus.apt.EventBusIndex;


public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);
        EventBus.getDefault().addIndex(new EventBusIndex());

    }


    @Subscribe
    public void event(PostEvent event){
        Log.d(TAG,"event666:" + event.toString());
    }


    @Subscribe
    public void test(TestEvent event){
        Log.d(TAG,"event666:" + event.toString());
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

       EventBus.getDefault().unRegister(this);

    }

    public void jump(View view) {

        EventBus.getDefault().postSticky(new TestEvent("sticky",23));

        startActivity(new Intent(this,SecondActivity.class));
    }
}
