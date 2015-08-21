package com.cerevo.blueninja.bletimesetter;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    private Calendar mCalendarLocal;
    private Handler mHandler;
    private Timer mLocalTime;

    private TextView mtextViewLocalTimeValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mtextViewLocalTimeValue = (TextView)findViewById(R.id.textViewLocalTimeValue);

        mCalendarLocal = Calendar.getInstance();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                mCalendarLocal.setTimeInMillis(System.currentTimeMillis());
                int year = mCalendarLocal.get(Calendar.YEAR);
                int mon = mCalendarLocal.get(Calendar.MONTH) + 1;
                int mday = mCalendarLocal.get(Calendar.DAY_OF_MONTH);
                int wday = mCalendarLocal.get(Calendar.DAY_OF_WEEK);
                int hour = mCalendarLocal.get(Calendar.HOUR_OF_DAY);
                int min = mCalendarLocal.get(Calendar.MINUTE);
                int sec = mCalendarLocal.get(Calendar.SECOND);

                mtextViewLocalTimeValue.setText(
                        String.format(
                                "%04d/%02d/%02d %02d:%02d:%02d",
                                year, mon, mday,
                                hour, min, sec));
            }
        };

        mLocalTime = new Timer();
        mLocalTime.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                mHandler.sendMessage(msg);
            }
        }, 0, 1000);

    }
}
