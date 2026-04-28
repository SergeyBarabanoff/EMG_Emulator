package com.example.emgphone;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmgSocketClient {

    public interface HostProvider {
        String getHost();
    }

    public interface PortProvider {
        int getPort();
    }

    public interface CommandListener {
        void onCommandReceived(PhoneCommand command);
    }

    public interface StatusListener {
        void onStatus(String status);
    }

    private final HostProvider hostProvider;
    private final PortProvider portProvider;
    private final CommandListener commandListener;
    private final StatusListener statusListener;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

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

    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

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