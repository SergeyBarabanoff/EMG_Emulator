package com.example.emgphone;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity used for manual keyboard-based testing of cursor control.
 *
 * <p>This screen captures hardware keyboard input and converts supported keys into
 * {@link PhoneCommand} broadcasts consumed by {@link EmgAccessibilityService}. It is intended as
 * a debug or testing harness for verifying cursor movement and command handling without EMG input.</p>
 */
public class KeyboardTestActivity extends AppCompatActivity {

    /** Displays the most recent keyboard action sent to the accessibility service. */
    private TextView statusTextView;

    /** Root focusable view used to receive keyboard input. */
    private View rootView;

    /**
     * Initializes the keyboard test UI and requests focus so hardware key events are received.
     *
     * @param savedInstanceState previously saved state, or {@code null} if none exists
     */
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

    /**
     * Re-requests focus when the activity resumes so that keyboard input keeps working.
     */
    @Override
    protected void onResume() {
        super.onResume();
        rootView.requestFocus();
    }

    /**
     * Intercepts key presses and translates supported keys into {@link PhoneCommand} values.
     *
     * <p>Supported keys:
     * <ul>
     *     <li>W - move up</li>
     *     <li>A - move left</li>
     *     <li>S - move down</li>
     *     <li>D - move right</li>
     *     <li>Space - toggle touch</li>
     *     <li>H - home</li>
     *     <li>R - recents</li>
     * </ul>
     * Any unsupported key is passed to the default implementation.</p>
     *
     * @param event key event dispatched to the activity
     * @return {@code true} if the event was handled, otherwise the superclass result
     */
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

    /**
     * Broadcasts a manual command to the accessibility service.
     *
     * @param command command to send
     */
    private void sendCommand(PhoneCommand command) {
        Intent intent = new Intent(EmgAccessibilityService.ACTION_MANUAL_COMMAND);
        intent.setPackage(getPackageName());
        intent.putExtra("command", command.name());
        sendBroadcast(intent);
    }
}