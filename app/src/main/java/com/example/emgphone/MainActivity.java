package com.example.emgphone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Main configuration activity for the EMG phone controller app.
 *
 * <p>This activity allows the user to:
 * <ul>
 *     <li>configure the host and port used to connect to the EMG classifier app,</li>
 *     <li>configure cursor movement step size,</li>
 *     <li>open Android accessibility settings,</li>
 *     <li>start and stop the built-in test-square sequence,</li>
 *     <li>open the keyboard-based test screen,</li>
 *     <li>view connection diagnostics updated by the socket client.</li>
 * </ul>
 *
 * <p>The activity itself is primarily a settings and testing UI. System-wide cursor control is
 * performed by {@link EmgAccessibilityService}.</p>
 */
public class MainActivity extends AppCompatActivity {

    /** Host/IP input field for the EMG bridge connection. */
    private EditText hostEditText;

    /** Port input field for the EMG bridge connection. */
    private EditText portEditText;

    /** Input field for configuring cursor step size in pixels. */
    private EditText cursorStepEditText;

    /** Button that saves the current settings. */
    private Button saveButton;

    /** Button that opens Android accessibility settings. */
    private Button openAccessibilityButton;

    /** Button that starts the built-in square-motion test. */
    private Button startTestSquareButton;

    /** Button that stops the built-in square-motion test. */
    private Button stopTestSquareButton;

    /** Button that opens the keyboard-based test activity. */
    private Button openKeyboardTestButton;

    /** Text view showing the current socket connection status. */
    private TextView connectionStatusTextView;

    /** Text view showing the most recent raw message received from the EMG bridge. */
    private TextView lastMessageTextView;

    /** Text view showing the most recent parsed command received from the EMG bridge. */
    private TextView lastCommandTextView;

    /** Main-thread handler used to refresh diagnostics periodically. */
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * Periodically refreshes the on-screen diagnostics from {@link ConnectionDiagnostics}.
     *
     * <p>This keeps the main screen updated with the latest socket state, last raw message, and
     * last parsed command.</p>
     */
    private final Runnable diagnosticsUpdater = new Runnable() {
        @Override
        public void run() {
            connectionStatusTextView.setText(ConnectionDiagnostics.socketStatus);
            lastMessageTextView.setText(ConnectionDiagnostics.lastMessage);
            lastCommandTextView.setText(ConnectionDiagnostics.lastCommand);
            uiHandler.postDelayed(this, 500);
        }
    };

    /**
     * Initializes the activity UI, loads saved preferences, and wires up button actions.
     *
     * @param savedInstanceState previously saved state, or {@code null} if none exists
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostEditText = findViewById(R.id.hostEditText);
        portEditText = findViewById(R.id.portEditText);
        cursorStepEditText = findViewById(R.id.cursorStepEditText);
        saveButton = findViewById(R.id.saveButton);
        openAccessibilityButton = findViewById(R.id.openAccessibilityButton);
        startTestSquareButton = findViewById(R.id.startTestSquareButton);
        stopTestSquareButton = findViewById(R.id.stopTestSquareButton);

        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        lastMessageTextView = findViewById(R.id.lastMessageTextView);
        lastCommandTextView = findViewById(R.id.lastCommandTextView);

        loadPrefs();

        saveButton.setOnClickListener(v -> {
            savePrefs();
            Intent intent = new Intent(EmgAccessibilityService.ACTION_RELOAD_SETTINGS);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        });

        openAccessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        startTestSquareButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmgAccessibilityService.ACTION_START_TEST_SQUARE);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        });

        stopTestSquareButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmgAccessibilityService.ACTION_STOP_TEST_SQUARE);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        });

        openKeyboardTestButton = findViewById(R.id.openKeyboardTestButton);
        openKeyboardTestButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, KeyboardTestActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Starts periodic diagnostics updates when the activity becomes visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(diagnosticsUpdater);
    }

    /**
     * Stops periodic diagnostics updates when the activity is no longer in the foreground.
     */
    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(diagnosticsUpdater);
    }

    /**
     * Loads saved host, port, and cursor-step settings into the UI controls.
     *
     * <p>If no saved values exist, built-in defaults are used.</p>
     */
    private void loadPrefs() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(
                EmgAccessibilityService.PREFS_NAME,
                Context.MODE_PRIVATE
        );

        String host = prefs.getString(EmgAccessibilityService.KEY_HOST, "10.0.2.2");
        int port = prefs.getInt(EmgAccessibilityService.KEY_PORT, 8765);
        int cursorStep = prefs.getInt(EmgAccessibilityService.KEY_CURSOR_STEP, 60);

        hostEditText.setText(host);
        portEditText.setText(String.valueOf(port));
        cursorStepEditText.setText(String.valueOf(cursorStep));
    }

    /**
     * Reads settings from the UI, validates them, and persists them to shared preferences.
     *
     * <p>If the host is blank or the numeric fields cannot be parsed, default fallback values are
     * used instead.</p>
     */
    private void savePrefs() {
        String host = hostEditText.getText().toString().trim();
        if (host.isEmpty()) {
            host = "10.0.2.2";
        }

        int port;
        try {
            port = Integer.parseInt(portEditText.getText().toString().trim());
        } catch (Exception e) {
            port = 8765;
        }

        int cursorStep;
        try {
            cursorStep = Integer.parseInt(cursorStepEditText.getText().toString().trim());
        } catch (Exception e) {
            cursorStep = 60;
        }

        SharedPreferences prefs = getSharedPreferences(
                EmgAccessibilityService.PREFS_NAME,
                Context.MODE_PRIVATE
        );

        prefs.edit()
                .putString(EmgAccessibilityService.KEY_HOST, host)
                .putInt(EmgAccessibilityService.KEY_PORT, port)
                .putInt(EmgAccessibilityService.KEY_CURSOR_STEP, cursorStep)
                .apply();
    }
}