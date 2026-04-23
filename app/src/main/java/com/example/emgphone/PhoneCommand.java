package com.example.emgphone;

public enum PhoneCommand {
    MOVE_UP,
    MOVE_DOWN,
    MOVE_LEFT,
    MOVE_RIGHT,
    TAP,
    HOME,
    BACK,
    RECENTS,
    NOTIFICATIONS,
    UNKNOWN;

    public static PhoneCommand fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        for (PhoneCommand command : PhoneCommand.values()) {
            if (command.name().equals(value)) {
                return command;
            }
        }

        return UNKNOWN;
    }
}