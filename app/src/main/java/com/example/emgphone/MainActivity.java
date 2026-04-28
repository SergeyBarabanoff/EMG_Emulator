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

public class MainActivity extends AppCompatActivity {

    private EditText hostEditText;
    private EditText portEditText;
    private EditText cursorStepEditText;
    private Button saveButton;
    private Button openAccessibilityButton;
    private Button startTestSquareButton;
    private Button stopTestSquareButton;
    private Button openKeyboardTestButton;

    private TextView connectionStatusTextView;
    private TextView lastMessageTextView;
    private TextView lastCommandTextView;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable diagnosticsUpdater = new Runnable() {
        @Override
        public void run() {
            connectionStatusTextView.setText(ConnectionDiagnostics.socketStatus);
            lastMessageTextView.setText(ConnectionDiagnostics.lastMessage);
            lastCommandTextView.setText(ConnectionDiagnostics.lastCommand);
            uiHandler.postDelayed(this, 500);
        }
    };

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
            sendBroadcast(intent);
        });

        openAccessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        startTestSquareButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmgAccessibilityService.ACTION_START_TEST_SQUARE);
            sendBroadcast(intent);
        });

        stopTestSquareButton.setOnClickListener(v -> {
            Intent intent = new Intent(EmgAccessibilityService.ACTION_STOP_TEST_SQUARE);
            sendBroadcast(intent);
        });

        openKeyboardTestButton = findViewById(R.id.openKeyboardTestButton);
        openKeyboardTestButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, KeyboardTestActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHandler.post(diagnosticsUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHandler.removeCallbacks(diagnosticsUpdater);
    }

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