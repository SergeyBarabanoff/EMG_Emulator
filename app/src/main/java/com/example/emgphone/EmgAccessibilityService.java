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

/**
 * Accessibility service that drives system-wide phone control for the EMG controller app.
 *
 * <p>This service is the core runtime component that:
 * <ul>
 *     <li>creates and maintains a visible overlay cursor,</li>
 *     <li>receives commands from the EMG socket client and local test tools,</li>
 *     <li>moves the virtual cursor on screen,</li>
 *     <li>dispatches touch gestures, including touch-hold toggling,</li>
 *     <li>supports local debugging features such as the test-square sequence.</li>
 * </ul>
 *
 * <p>The service is designed so that both external EMG input and internal manual test input
 * flow through the same command-handling path.
 */
public class EmgAccessibilityService extends AccessibilityService {

    /** SharedPreferences file name used for app settings. */
    public static final String PREFS_NAME = "emg_phone_prefs";

    /** SharedPreferences key for the EMG bridge host. */
    public static final String KEY_HOST = "host";

    /** SharedPreferences key for the EMG bridge port. */
    public static final String KEY_PORT = "port";

    /** SharedPreferences key for the cursor step size in pixels. */
    public static final String KEY_CURSOR_STEP = "cursor_step";

    /** Broadcast action requesting that socket settings be reloaded and the client reconnected. */
    public static final String ACTION_RELOAD_SETTINGS = "com.example.emgphone.RELOAD_SETTINGS";

    /** Broadcast action requesting that the local square-motion test begin. */
    public static final String ACTION_START_TEST_SQUARE = "com.example.emgphone.START_TEST_SQUARE";

    /** Broadcast action requesting that the local square-motion test stop. */
    public static final String ACTION_STOP_TEST_SQUARE = "com.example.emgphone.STOP_TEST_SQUARE";

    /** Broadcast action used to send a manual command from test UI components. */
    public static final String ACTION_MANUAL_COMMAND = "com.example.emgphone.MANUAL_COMMAND";

    /** Window manager used to display and update the accessibility overlay. */
    private WindowManager windowManager;

    /** Root overlay container holding the cursor visuals and status label. */
    private FrameLayout overlayRoot;

    /** Main circular cursor marker representing the virtual finger location. */
    private View cursorDot;

    /** Horizontal crosshair line for improving cursor visibility. */
    private View cursorHorizontal;

    /** Vertical crosshair line for improving cursor visibility. */
    private View cursorVertical;

    /** Status text shown on the overlay for diagnostics and state updates. */
    private TextView statusTextView;

    /** Layout parameters for the overlay window. */
    private WindowManager.LayoutParams overlayParams;

    /** Main-thread handler used to safely update UI-related service state. */
    private Handler mainHandler;

    /** TCP client that receives commands from the EMG classifier application. */
    private EmgSocketClient socketClient;

    /** Current horizontal cursor position in pixels. */
    private int cursorX = 300;

    /** Current vertical cursor position in pixels. */
    private int cursorY = 500;

    /** Diameter of the circular cursor marker in pixels. */
    private int cursorSizePx = 72;

    /** Length of each crosshair line in pixels. */
    private int crosshairLengthPx = 120;

    /** Whether the built-in square test is currently running. */
    private boolean testSquareRunning = false;

    /** Background thread that runs the built-in square test sequence. */
    private Thread testSquareThread;

    /** Whether a touch-hold is currently active. */
    private boolean touchHeld = false;

    /**
     * Stroke object representing the active held touch, used so the touch can later be released.
     */
    private GestureDescription.StrokeDescription heldStroke = null;

    /**
     * Broadcast receiver that handles control actions from the main activity and keyboard test UI.
     *
     * <p>Supported broadcasts include settings reload, test-square start/stop, and manual command
     * dispatch.
     */
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

    /**
     * Called when the accessibility service is connected and ready to start work.
     *
     * <p>This initializes the overlay, registers internal control broadcasts, starts the socket
     * client, and updates the visible service status.
     */
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
        registerReceiver(controlReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        reconnectSocket();
        updateStatus("Service started");
    }

    /**
     * Receives accessibility events from the system.
     *
     * <p>This implementation does not currently use incoming accessibility events, because control
     * is driven by socket input and internal test input rather than by analyzing UI events.</p>
     *
     * @param event accessibility event supplied by the system
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    /**
     * Called when the accessibility service is interrupted by the system.
     *
     * <p>This updates the on-screen status so interruptions are visible during debugging.</p>
     */
    @Override
    public void onInterrupt() {
        updateStatus("Service interrupted");
    }

    /**
     * Cleans up service resources when the service is being destroyed.
     *
     * <p>This unregisters the internal broadcast receiver, stops the test sequence, stops the
     * socket client, and removes the overlay from the window manager.</p>
     */
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

    /**
     * Recreates the socket client using the latest stored settings and starts it.
     *
     * <p>If an existing socket client is running, it is stopped first.</p>
     */
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

    /**
     * Returns the shared preferences object containing app settings.
     *
     * @return shared preferences for EMG phone settings
     */
    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the configured EMG bridge host.
     *
     * @return host name or IP address, defaulting to {@code 10.0.2.2}
     */
    private String getHost() {
        return getPrefs().getString(KEY_HOST, "10.0.2.2");
    }

    /**
     * Returns the configured EMG bridge port.
     *
     * @return socket port, defaulting to {@code 8765}
     */
    private int getPort() {
        return getPrefs().getInt(KEY_PORT, 8765);
    }

    /**
     * Returns the configured cursor movement step size.
     *
     * @return cursor step size in pixels, defaulting to {@code 60}
     */
    private int getCursorStep() {
        return getPrefs().getInt(KEY_CURSOR_STEP, 60);
    }

    /**
     * Creates the full-screen accessibility overlay containing the cursor and diagnostics text.
     *
     * <p>The overlay includes:
     * <ul>
     *     <li>a circular cursor dot,</li>
     *     <li>horizontal and vertical crosshair lines,</li>
     *     <li>a small status label in the upper-left corner.</li>
     * </ul>
     */
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

    /**
     * Updates the overlay status label with the supplied text and current touch-hold state.
     *
     * @param text status message to display
     */
    private void updateStatus(String text) {
        if (statusTextView != null) {
            String suffix = touchHeld ? " | TOUCH HELD" : "";
            statusTextView.setText(text + suffix);
        }
    }

    /**
     * Repositions the visual cursor elements to match the current cursor coordinates.
     *
     * <p>This updates the dot and crosshair lines so the cursor remains easy to locate on screen.</p>
     */
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

    /**
     * Executes a high-level phone command.
     *
     * <p>Movement commands reposition the cursor, tap toggles the touch-hold state, and system
     * commands invoke global accessibility actions such as Home or Recents.</p>
     *
     * @param command command to execute
     */
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

    /**
     * Moves the cursor by the specified delta while keeping it within screen bounds.
     *
     * @param dx horizontal movement delta in pixels
     * @param dy vertical movement delta in pixels
     */
    private void moveCursor(int dx, int dy) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int maxX = Math.max(0, displayMetrics.widthPixels - cursorSizePx);
        int maxY = Math.max(0, displayMetrics.heightPixels - cursorSizePx);

        cursorX = Math.min(Math.max(cursorX + dx, 0), maxX);
        cursorY = Math.min(Math.max(cursorY + dy, 0), maxY);

        updateCursorVisuals();
        updateStatus("Cursor: (" + cursorX + ", " + cursorY + ")");
    }

    /**
     * Toggles between starting and ending a held touch gesture.
     *
     * <p>If no touch is currently active, a touch-down gesture is started. If a touch is already
     * held, the gesture is released.</p>
     */
    private void toggleTouchHold() {
        if (!touchHeld) {
            startTouchHold();
        } else {
            endTouchHold();
        }
    }

    /**
     * Starts a long-running touch gesture at the current cursor position.
     *
     * <p>This simulates a finger being placed on the screen and held down until
     * {@link #endTouchHold()} is called.</p>
     */
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

    /**
     * Ends the currently active held touch gesture.
     *
     * <p>If there is no active held touch, the method simply clears the state and reports that no
     * held touch was available to release.</p>
     */
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

    /**
     * Starts the built-in square-motion test in a background thread.
     *
     * <p>The square test repeatedly moves the cursor in a square and toggles touch at the corners
     * to verify motion and touch behavior without external EMG input.</p>
     */
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

    /**
     * Stops the background square-motion test if it is currently running.
     */
    private void stopTestSquare() {
        testSquareRunning = false;
        if (testSquareThread != null) {
            testSquareThread.interrupt();
            testSquareThread = null;
        }
    }

    /**
     * Executes one complete square test cycle.
     *
     * <p>The sequence toggles touch at each corner and moves the cursor along all four sides of
     * the square.</p>
     *
     * @throws InterruptedException if the sleep between commands is interrupted
     */
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

    /**
     * Repeats a movement command a fixed number of times with a delay between each step.
     *
     * @param command movement command to repeat
     * @param count number of repetitions
     * @param delayMs delay between repetitions in milliseconds
     * @throws InterruptedException if interrupted during the delay
     */
    private void repeatMove(PhoneCommand command, int count, long delayMs) throws InterruptedException {
        for (int i = 0; i < count && testSquareRunning; i++) {
            performTestCommand(command);
            Thread.sleep(delayMs);
        }
    }

    /**
     * Posts a test command onto the main thread so it is executed through the standard command
     * handling path.
     *
     * @param command command to perform
     */
    private void performTestCommand(PhoneCommand command) {
        mainHandler.post(() -> handleCommand(command));
    }
}