package com.example.id.delovnapraksa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ivanvelickovski on 1/29/17.
 */

public class BootReceiver extends BroadcastReceiver {
    MailAlarmReceiver alarm = new MailAlarmReceiver();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            // restart receiver when mobile restarts
            Log.d("broadcast receiver", "boot received");
            alarm.setAlarm(context);
        }
    }
}
