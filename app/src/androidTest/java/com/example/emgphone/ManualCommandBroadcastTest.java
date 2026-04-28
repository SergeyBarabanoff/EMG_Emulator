package com.example.emgphone;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ManualCommandBroadcastTest {

    @Test
    public void sendsManualMoveRightBroadcast() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = new Intent(EmgAccessibilityService.ACTION_MANUAL_COMMAND);
        intent.putExtra("command", PhoneCommand.MOVE_RIGHT.name());
        context.sendBroadcast(intent);
    }

    @Test
    public void sendsManualTapBroadcast() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = new Intent(EmgAccessibilityService.ACTION_MANUAL_COMMAND);
        intent.putExtra("command", PhoneCommand.TAP.name());
        context.sendBroadcast(intent);
    }
}