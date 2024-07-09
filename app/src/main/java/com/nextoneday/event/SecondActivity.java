package com.nextoneday.event;


import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.nextoneday.event.event.PostEvent;
import com.nextoneday.event.event.TestEvent;
import com.nextoneday.eventbus.EventBus;

import com.nextoneday.annoation.Subscribe;

public class SecondActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        EventBus.getDefault().register(this);

    }

    @Subscribe(sticky = true)
    public void stickEvent(TestEvent event){
        Log.d("stickEvent", event.toString());
    }

    public void post(View view) {
            EventBus.getDefault().post(new PostEvent("post",666));
//            EventBus.getDefault().post1(new TestEvent("test",1231));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unRegister(this);

    }


}
