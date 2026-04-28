package com.example.emgphone;

/**
 * Represents the position of a virtual cursor on the screen.
 *
 * <p>This class stores the cursor's current top-left position in pixels and provides bounded
 * movement logic so the cursor remains visible within the screen dimensions supplied to
 * {@link #move(int, int, int, int)}.</p>
 */
public class CursorState {
    /** Current horizontal cursor position in pixels. */
    private int x;

    /** Current vertical cursor position in pixels. */
    private int y;

    /** Size of the cursor in pixels, used to keep it within screen bounds. */
    private final int cursorSizePx;

    /**
     * Creates a new cursor state.
     *
     * @param startX initial horizontal position in pixels
     * @param startY initial vertical position in pixels
     * @param cursorSizePx cursor size in pixels
     */
    public CursorState(int startX, int startY, int cursorSizePx) {
        this.x = startX;
        this.y = startY;
        this.cursorSizePx = cursorSizePx;
    }

    /**
     * Moves the cursor by the given offset while clamping it to the visible screen area.
     *
     * <p>The maximum allowed position is reduced by the cursor size so that the entire cursor
     * remains on screen.</p>
     *
     * @param dx horizontal movement delta in pixels
     * @param dy vertical movement delta in pixels
     * @param screenWidth current screen width in pixels
     * @param screenHeight current screen height in pixels
     */
    public void move(int dx, int dy, int screenWidth, int screenHeight) {
        int maxX = Math.max(0, screenWidth - cursorSizePx);
        int maxY = Math.max(0, screenHeight - cursorSizePx);

        x = Math.min(Math.max(x + dx, 0), maxX);
        y = Math.min(Math.max(y + dy, 0), maxY);
    }

    /**
     * Returns the current horizontal cursor position.
     *
     * @return x-coordinate in pixels
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the current vertical cursor position.
     *
     * @return y-coordinate in pixels
     */
    public int getY() {
        return y;
    }
}