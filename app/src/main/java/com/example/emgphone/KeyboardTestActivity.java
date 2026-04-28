package com.example.emgphone;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class KeyboardTestActivity extends AppCompatActivity {

    private TextView statusTextView;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_test);

        statusTextView = findViewById(R.id.statusTextView);
        rootView = findViewById(R.id.keyboardTestRoot);

        rootView.setFocusable(true);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();

        statusTextView.setText("Keyboard test active. Click here, then use W/A/S/D, Space, H, R.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        rootView.requestFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }

        PhoneCommand command = null;
        String label = null;

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_W:
                command = PhoneCommand.MOVE_UP;
                label = "MOVE_UP";
                break;
            case KeyEvent.KEYCODE_A:
                command = PhoneCommand.MOVE_LEFT;
                label = "MOVE_LEFT";
                break;
            case KeyEvent.KEYCODE_S:
                command = PhoneCommand.MOVE_DOWN;
                label = "MOVE_DOWN";
                break;
            case KeyEvent.KEYCODE_D:
                command = PhoneCommand.MOVE_RIGHT;
                label = "MOVE_RIGHT";
                break;
            case KeyEvent.KEYCODE_SPACE:
                command = PhoneCommand.TAP;
                label = "TOUCH TOGGLE";
                break;
            case KeyEvent.KEYCODE_H:
                command = PhoneCommand.HOME;
                label = "HOME";
                break;
            case KeyEvent.KEYCODE_R:
                command = PhoneCommand.RECENTS;
                label = "RECENTS";
                break;
        }

        if (command != null) {
            sendCommand(command);
            statusTextView.setText("Sent: " + label);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void sendCommand(PhoneCommand command) {
        Intent intent = new Intent(EmgAccessibilityService.ACTION_MANUAL_COMMAND);
        intent.putExtra("command", command.name());
        sendBroadcast(intent);
    }
}