package com.example.emgphone;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

public class EmgAccessibilityService extends AccessibilityService {

    public static final String PREFS_NAME = "emg_phone_prefs";
    public static final String KEY_HOST = "host";
    public static final String KEY_PORT = "port";
    public static final String KEY_CURSOR_STEP = "cursor_step";
    public static final String ACTION_RELOAD_SETTINGS = "com.example.emgphone.RELOAD_SETTINGS";

    private WindowManager windowManager;
    private View overlayRoot;
    private View cursorDot;
    private TextView statusTextView;
    private WindowManager.LayoutParams overlayParams;

    private Handler mainHandler;
    private EmgSocketClient socketClient;

    private int cursorX = 300;
    private int cursorY = 500;
    private int cursorSizePx = 40;
    private boolean paused = false;

    public static final String ACTION_START_TEST_SQUARE = "com.example.emgphone.START_TEST_SQUARE";
    public static final String ACTION_STOP_TEST_SQUARE = "com.example.emgphone.STOP_TEST_SQUARE";
    private boolean testSquareRunning = false;
    private Thread testSquareThread;
    private final BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            String action = intent.getAction();

            if (ACTION_RELOAD_SETTINGS.equals(action)) {
                reconnectSocket();
            } else if (ACTION_START_TEST_SQUARE.equals(action)) {
                startTestSquare();
            } else if (ACTION_STOP_TEST_SQUARE.equals(action)) {
                stopTestSquare();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        mainHandler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        createOverlay();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RELOAD_SETTINGS);
        filter.addAction(ACTION_START_TEST_SQUARE);
        filter.addAction(ACTION_STOP_TEST_SQUARE);
        registerReceiver(controlReceiver, filter);

        reconnectSocket();
        updateStatus("Service started");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        updateStatus("Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(controlReceiver);
        } catch (Exception ignored) {
        }

        stopTestSquare();

        if (socketClient != null) {
            socketClient.stop();
        }

        try {
            if (overlayRoot != null) {
                windowManager.removeView(overlayRoot);
            }
        } catch (Exception ignored) {
        }
    }

    private void reconnectSocket() {
        if (socketClient != null) {
            socketClient.stop();
        }

        socketClient = new EmgSocketClient(
                this::getHost,
                this::getPort,
                command -> mainHandler.post(() -> {
                    if (!paused) {
                        handleCommand(command);
                    }
                }),
                status -> mainHandler.post(() -> updateStatus(status))
        );

        socketClient.start();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String getHost() {
        return getPrefs().getString(KEY_HOST, "10.0.2.2");
    }

    private int getPort() {
        return getPrefs().getInt(KEY_PORT, 8765);
    }

    private int getCursorStep() {
        return getPrefs().getInt(KEY_CURSOR_STEP, 60);
    }

    private void createOverlay() {
        FrameLayout container = new FrameLayout(this);

        cursorDot = new View(this);
        cursorDot.setBackgroundColor(Color.RED);

        FrameLayout.LayoutParams cursorLayoutParams =
                new FrameLayout.LayoutParams(cursorSizePx, cursorSizePx);
        container.addView(cursorDot, cursorLayoutParams);

        statusTextView = new TextView(this);
        statusTextView.setTextColor(Color.WHITE);
        statusTextView.setBackgroundColor(0x99000000);
        statusTextView.setPadding(16, 8, 16, 8);
        statusTextView.setText("Waiting...");
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);

        FrameLayout.LayoutParams statusLayoutParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
        statusLayoutParams.leftMargin = 20;
        statusLayoutParams.topMargin = 60;
        container.addView(statusTextView, statusLayoutParams);

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = cursorX;
        overlayParams.y = cursorY;

        overlayRoot = container;
        windowManager.addView(overlayRoot, overlayParams);
    }

    private void updateStatus(String text) {
        if (statusTextView != null) {
            statusTextView.setText(text);
        }
    }

    private void handleCommand(PhoneCommand command) {
        switch (command) {
            case MOVE_UP:
                moveCursor(0, -getCursorStep());
                break;
            case MOVE_DOWN:
                moveCursor(0, getCursorStep());
                break;
            case MOVE_LEFT:
                moveCursor(-getCursorStep(), 0);
                break;
            case MOVE_RIGHT:
                moveCursor(getCursorStep(), 0);
                break;
            case TAP:
                tapAtCursor();
                break;
            case HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case RECENTS:
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
            case NOTIFICATIONS:
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                break;
            case UNKNOWN:
            default:
                updateStatus("Unknown command");
                break;
        }
    }

    private void moveCursor(int dx, int dy) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int maxX = Math.max(0, displayMetrics.widthPixels - cursorSizePx);
        int maxY = Math.max(0, displayMetrics.heightPixels - cursorSizePx);

        cursorX = Math.min(Math.max(cursorX + dx, 0), maxX);
        cursorY = Math.min(Math.max(cursorY + dy, 0), maxY);

        overlayParams.x = cursorX;
        overlayParams.y = cursorY;

        if (overlayRoot != null) {
            windowManager.updateViewLayout(overlayRoot, overlayParams);
        }

        updateStatus("Cursor: (" + cursorX + ", " + cursorY + ")");
    }

    private void tapAtCursor() {
        float tapX = cursorX + (cursorSizePx / 2f);
        float tapY = cursorY + (cursorSizePx / 2f);

        Path path = new Path();
        path.moveTo(tapX, tapY);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 60);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
        updateStatus("Tap at (" + tapX + ", " + tapY + ")");
    }

    private void startTestSquare() {
        if (testSquareRunning) {
            updateStatus("Test square already running");
            return;
        }

        testSquareRunning = true;

        testSquareThread = new Thread(() -> {
            try {
                while (testSquareRunning) {
                    runTestSquareOnce();
                    Thread.sleep(1200);
                }
            } catch (InterruptedException ignored) {
            } finally {
                testSquareRunning = false;
                mainHandler.post(() -> updateStatus("Test square stopped"));
            }
        });

        testSquareThread.setDaemon(true);
        testSquareThread.start();
        updateStatus("Test square started");
    }

    private void stopTestSquare() {
        testSquareRunning = false;
        if (testSquareThread != null) {
            testSquareThread.interrupt();
            testSquareThread = null;
        }
    }

    private void runTestSquareOnce() throws InterruptedException {
        int stepsPerSide = 8;
        long moveDelayMs = 250;
        long tapDelayMs = 450;

        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(tapDelayMs);

        repeatMove(PhoneCommand.MOVE_RIGHT, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(tapDelayMs);

        repeatMove(PhoneCommand.MOVE_DOWN, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(tapDelayMs);

        repeatMove(PhoneCommand.MOVE_LEFT, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(tapDelayMs);

        repeatMove(PhoneCommand.MOVE_UP, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(tapDelayMs);
    }

    private void repeatMove(PhoneCommand command, int count, long delayMs) throws InterruptedException {
        for (int i = 0; i < count && testSquareRunning; i++) {
            performTestCommand(command);
            Thread.sleep(delayMs);
        }
    }

    private void performTestCommand(PhoneCommand command) {
        mainHandler.post(() -> handleCommand(command));
    }
}