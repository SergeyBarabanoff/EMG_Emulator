package com.example.emgphone;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP client that connects the Android app to the external EMG classifier app.
 *
 * <p>This client continuously attempts to connect to the configured host and port, reads
 * newline-delimited JSON messages, updates connection diagnostics, and forwards parsed commands
 * and status messages to listeners.</p>
 */
public class EmgSocketClient {

    /**
     * Supplies the current host name or IP address to use for the socket connection.
     */
    public interface HostProvider {
        /**
         * Returns the current host name or IP address.
         *
         * @return socket host
         */
        String getHost();
    }

    /**
     * Supplies the current port to use for the socket connection.
     */
    public interface PortProvider {
        /**
         * Returns the current socket port.
         *
         * @return socket port
         */
        int getPort();
    }

    /**
     * Listener notified when a valid {@link PhoneCommand} is received from the EMG app.
     */
    public interface CommandListener {
        /**
         * Handles a parsed phone command.
         *
         * @param command parsed command value
         */
        void onCommandReceived(PhoneCommand command);
    }

    /**
     * Listener notified when status or diagnostic information is available.
     */
    public interface StatusListener {
        /**
         * Handles a connection or message status update.
         *
         * @param status human-readable status string
         */
        void onStatus(String status);
    }

    /** Provides the current host for the socket connection. */
    private final HostProvider hostProvider;

    /** Provides the current port for the socket connection. */
    private final PortProvider portProvider;

    /** Receives parsed command messages. */
    private final CommandListener commandListener;

    /** Receives human-readable status updates. */
    private final StatusListener statusListener;

    /** Tracks whether the client should keep trying to connect and read messages. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Background worker thread that owns the socket lifecycle. */
    private Thread workerThread;

    /**
     * Creates a new EMG socket client.
     *
     * @param hostProvider provider for the current host
     * @param portProvider provider for the current port
     * @param commandListener listener for parsed command messages
     * @param statusListener listener for connection and parsing status messages
     */
    public EmgSocketClient(
            HostProvider hostProvider,
            PortProvider portProvider,
            CommandListener commandListener,
            StatusListener statusListener
    ) {
        this.hostProvider = hostProvider;
        this.portProvider = portProvider;
        this.commandListener = commandListener;
        this.statusListener = statusListener;
    }

    /**
     * Starts the socket client if it is not already running.
     *
     * <p>This method launches a background daemon thread that repeatedly attempts to connect to
     * the EMG app, read incoming messages, and reconnect after failures.</p>
     */
    public void start() {
        if (running.get()) {
            return;
        }

        running.set(true);

        workerThread = new Thread(() -> {
            while (running.get()) {
                String host = hostProvider.getHost();
                int port = portProvider.getPort();

                try {
                    ConnectionDiagnostics.socketStatus = "Socket: connecting to " + host + ":" + port;
                    statusListener.onStatus(ConnectionDiagnostics.socketStatus);

                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 3000);

                    ConnectionDiagnostics.socketStatus = "Socket: connected to " + host + ":" + port;
                    statusListener.onStatus(ConnectionDiagnostics.socketStatus);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );

                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        ConnectionDiagnostics.lastMessage = "Last message: " + line;
                        handleLine(line);
                    }

                    ConnectionDiagnostics.socketStatus = "Socket: disconnected";
                    statusListener.onStatus(ConnectionDiagnostics.socketStatus);
                    socket.close();

                } catch (Exception e) {
                    ConnectionDiagnostics.socketStatus = "Socket error: " + e.getMessage();
                    statusListener.onStatus(ConnectionDiagnostics.socketStatus);

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        });

        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Stops the socket client and interrupts the background worker thread.
     */
    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    /**
     * Parses a single JSON message line received from the socket.
     *
     * <p>Recognized message types are:
     * <ul>
     *     <li>{@code command} - converted to a {@link PhoneCommand}</li>
     *     <li>{@code status} - forwarded as a status message</li>
     *     <li>{@code hello} - forwarded as a hello message</li>
     *     <li>{@code heartbeat} - forwarded as a heartbeat message</li>
     * </ul>
     * Unrecognized or malformed messages are reported to the status listener.</p>
     *
     * @param line one newline-delimited JSON message from the socket
     */
    private void handleLine(String line) {
        try {
            JSONObject json = new JSONObject(line);
            String type = json.optString("type", "");

            if ("command".equals(type)) {
                String rawCommand = json.optString("command", "");
                PhoneCommand command = PhoneCommand.fromString(rawCommand);
                ConnectionDiagnostics.lastCommand = "Last command: " + command.name();
                commandListener.onCommandReceived(command);

            } else if ("status".equals(type)) {
                statusListener.onStatus("Status msg: " + json.optString("message", ""));

            } else if ("hello".equals(type)) {
                statusListener.onStatus("Hello msg: " + json.optString("message", ""));

            } else if ("heartbeat".equals(type)) {
                statusListener.onStatus("Heartbeat received");

            } else {
                statusListener.onStatus("Unknown msg type: " + type);
            }

        } catch (Exception e) {
            statusListener.onStatus("Bad message: " + e.getMessage());
        }
    }
}