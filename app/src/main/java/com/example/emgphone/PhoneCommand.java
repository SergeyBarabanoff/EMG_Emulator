package com.example.emgphone;

/**
 * Enumerates the high-level commands that can be sent to the phone controller.
 *
 * <p>These commands are used by both manual test input and EMG-driven socket input to control the
 * accessibility service consistently.</p>
 */
public enum PhoneCommand {
    /** Move the virtual cursor upward. */
    MOVE_UP,

    /** Move the virtual cursor downward. */
    MOVE_DOWN,

    /** Move the virtual cursor to the left. */
    MOVE_LEFT,

    /** Move the virtual cursor to the right. */
    MOVE_RIGHT,

    /** Toggle the touch state, such as finger down or finger up. */
    TAP,

    /** Trigger the Android home action. */
    HOME,

    /** Trigger the Android back action. */
    BACK,

    /** Open the recent apps view. */
    RECENTS,

    /** Open the notifications shade. */
    NOTIFICATIONS,

    /** Fallback value for unknown or unsupported input. */
    UNKNOWN;

    /**
     * Converts a string into a {@link PhoneCommand}.
     *
     * <p>If the input is {@code null} or does not match any enum constant name exactly,
     * {@link #UNKNOWN} is returned.</p>
     *
     * @param value string representation of the command
     * @return matching command, or {@link #UNKNOWN} if no match exists
     */
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