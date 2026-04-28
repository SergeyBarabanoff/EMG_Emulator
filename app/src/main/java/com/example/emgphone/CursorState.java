package com.example.emgphone;

public class CursorState {
    private int x;
    private int y;
    private final int cursorSizePx;

    public CursorState(int startX, int startY, int cursorSizePx) {
        this.x = startX;
        this.y = startY;
        this.cursorSizePx = cursorSizePx;
    }

    public void move(int dx, int dy, int screenWidth, int screenHeight) {
        int maxX = Math.max(0, screenWidth - cursorSizePx);
        int maxY = Math.max(0, screenHeight - cursorSizePx);

        x = Math.min(Math.max(x + dx, 0), maxX);
        y = Math.min(Math.max(y + dy, 0), maxY);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}