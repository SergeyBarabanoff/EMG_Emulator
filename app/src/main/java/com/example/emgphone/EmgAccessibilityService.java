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
import android.graphics.drawable.GradientDrawable;
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
    public static final String ACTION_START_TEST_SQUARE = "com.example.emgphone.START_TEST_SQUARE";
    public static final String ACTION_STOP_TEST_SQUARE = "com.example.emgphone.STOP_TEST_SQUARE";
    public static final String ACTION_MANUAL_COMMAND = "com.example.emgphone.MANUAL_COMMAND";

    private WindowManager windowManager;
    private FrameLayout overlayRoot;
    private View cursorDot;
    private View cursorHorizontal;
    private View cursorVertical;
    private TextView statusTextView;
    private WindowManager.LayoutParams overlayParams;

    private Handler mainHandler;
    private EmgSocketClient socketClient;

    private int cursorX = 300;
    private int cursorY = 500;
    private int cursorSizePx = 72;
    private int crosshairLengthPx = 120;

    private boolean testSquareRunning = false;
    private Thread testSquareThread;

    private boolean touchHeld = false;
    private GestureDescription.StrokeDescription heldStroke = null;

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
            } else if (ACTION_MANUAL_COMMAND.equals(action)) {
                String raw = intent.getStringExtra("command");
                PhoneCommand command = PhoneCommand.fromString(raw);
                handleCommand(command);
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
        filter.addAction(ACTION_MANUAL_COMMAND);
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
                command -> mainHandler.post(() -> handleCommand(command)),
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
        overlayRoot = new FrameLayout(this);

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.START;

        cursorHorizontal = new View(this);
        cursorHorizontal.setBackgroundColor(Color.argb(220, 255, 0, 0));
        FrameLayout.LayoutParams horizontalParams = new FrameLayout.LayoutParams(
                crosshairLengthPx,
                6
        );
        overlayRoot.addView(cursorHorizontal, horizontalParams);

        cursorVertical = new View(this);
        cursorVertical.setBackgroundColor(Color.argb(220, 255, 0, 0));
        FrameLayout.LayoutParams verticalParams = new FrameLayout.LayoutParams(
                6,
                crosshairLengthPx
        );
        overlayRoot.addView(cursorVertical, verticalParams);

        cursorDot = new View(this);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(Color.argb(230, 255, 0, 0));
        dotDrawable.setStroke(4, Color.WHITE);
        cursorDot.setBackground(dotDrawable);

        FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(cursorSizePx, cursorSizePx);
        overlayRoot.addView(cursorDot, dotParams);

        statusTextView = new TextView(this);
        statusTextView.setTextColor(Color.WHITE);
        statusTextView.setBackgroundColor(0xAA000000);
        statusTextView.setPadding(20, 12, 20, 12);
        statusTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        statusTextView.setText("Waiting...");

        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.leftMargin = 24;
        statusParams.topMargin = 48;
        overlayRoot.addView(statusTextView, statusParams);

        windowManager.addView(overlayRoot, overlayParams);
        updateCursorVisuals();
    }

    private void updateStatus(String text) {
        if (statusTextView != null) {
            String suffix = touchHeld ? " | TOUCH HELD" : "";
            statusTextView.setText(text + suffix);
        }
    }

    private void updateCursorVisuals() {
        if (cursorDot == null || cursorHorizontal == null || cursorVertical == null) {
            return;
        }

        float dotLeft = cursorX;
        float dotTop = cursorY;
        float centerX = cursorX + (cursorSizePx / 2f);
        float centerY = cursorY + (cursorSizePx / 2f);

        cursorDot.setX(dotLeft);
        cursorDot.setY(dotTop);

        cursorHorizontal.setX(centerX - (crosshairLengthPx / 2f));
        cursorHorizontal.setY(centerY - 3);

        cursorVertical.setX(centerX - 3);
        cursorVertical.setY(centerY - (crosshairLengthPx / 2f));
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
                toggleTouchHold();
                break;
            case HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                updateStatus("HOME");
                break;
            case BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                updateStatus("BACK");
                break;
            case RECENTS:
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                updateStatus("RECENTS");
                break;
            case NOTIFICATIONS:
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                updateStatus("NOTIFICATIONS");
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

        updateCursorVisuals();
        updateStatus("Cursor: (" + cursorX + ", " + cursorY + ")");
    }

    private void toggleTouchHold() {
        if (!touchHeld) {
            startTouchHold();
        } else {
            endTouchHold();
        }
    }

    private void startTouchHold() {
        float tapX = cursorX + (cursorSizePx / 2f);
        float tapY = cursorY + (cursorSizePx / 2f);

        Path path = new Path();
        path.moveTo(tapX, tapY);

        heldStroke = new GestureDescription.StrokeDescription(path, 0, 60000, true);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(heldStroke)
                .build();

        boolean ok = dispatchGesture(gesture, null, null);
        if (ok) {
            touchHeld = true;
            updateStatus("Touch DOWN at (" + tapX + ", " + tapY + ")");
        } else {
            heldStroke = null;
            touchHeld = false;
            updateStatus("Touch DOWN failed");
        }
    }

    private void endTouchHold() {
        if (heldStroke == null) {
            touchHeld = false;
            updateStatus("No held touch to release");
            return;
        }

        float tapX = cursorX + (cursorSizePx / 2f);
        float tapY = cursorY + (cursorSizePx / 2f);

        Path path = new Path();
        path.moveTo(tapX, tapY);

        GestureDescription.StrokeDescription endStroke =
                heldStroke.continueStroke(path, 1, 1, false);

        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(endStroke)
                .build();

        boolean ok = dispatchGesture(gesture, null, null);
        touchHeld = false;
        heldStroke = null;

        if (ok) {
            updateStatus("Touch UP at (" + tapX + ", " + tapY + ")");
        } else {
            updateStatus("Touch UP failed");
        }
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
        long moveDelayMs = 220;
        long touchDelayMs = 250;

        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);

        repeatMove(PhoneCommand.MOVE_RIGHT, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);

        repeatMove(PhoneCommand.MOVE_DOWN, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);

        repeatMove(PhoneCommand.MOVE_LEFT, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);

        repeatMove(PhoneCommand.MOVE_UP, stepsPerSide, moveDelayMs);
        performTestCommand(PhoneCommand.TAP);
        Thread.sleep(touchDelayMs);
        performTestCommand(PhoneCommand.TAP);
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