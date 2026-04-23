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
                    statusListener.onStatus("Connecting to " + host + ":" + port);

                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 3000);
                    statusListener.onStatus("Connected to " + host + ":" + port);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );

                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        handleLine(line);
                    }

                    socket.close();

                } catch (Exception e) {
                    statusListener.onStatus("Disconnected: " + e.getMessage());

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
                commandListener.onCommandReceived(command);
            } else if ("status".equals(type)) {
                statusListener.onStatus(json.optString("message", ""));
            }

        } catch (Exception e) {
            statusListener.onStatus("Bad message: " + e.getMessage());
        }
    }
}