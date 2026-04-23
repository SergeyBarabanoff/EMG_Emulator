# EMG Phone Controller

This project allows a user to control an Android phone using EMG signals.

## Components

- Python EMG processing app (signal acquisition + classification)
- TCP bridge sending commands
- Android app with accessibility service controlling the UI

## Features

- Cursor movement
- Tap gestures
- Home / Recents control
- Emulator-based testing

## How to run

1. Start Python EMG app
2. Start Android emulator
3. Enable accessibility service
4. Run test sequence or EMG input

## Demo

Square movement test double:
- Moves cursor in square
- Taps at each corner