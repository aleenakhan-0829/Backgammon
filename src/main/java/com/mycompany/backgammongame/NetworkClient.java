/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * NetworkClient - Manages the TCP connection to the server.
 * Provides send() and a listener callback for incoming messages.
 */
public class NetworkClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // FIX #3 – volatile so the listener thread always sees the latest value
    private volatile Consumer<String> messageListener;

    private Thread listenerThread;
    private volatile boolean running = false;

    /**
     * Connect to the server. Blocks until the connection is established
     * (or throws IOException on failure).
     *
     * FIX #2 – accept the messageListener in the constructor so it is
     * guaranteed to be set BEFORE the listener thread starts, eliminating
     * the race condition where early messages were silently dropped.
     */
    public NetworkClient(String host, int port, Consumer<String> listener) throws IOException {
        // FIX #2 – set listener before the thread can fire
        this.messageListener = listener;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000); // 5-second timeout

        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        running = true;

        listenerThread = new Thread(this::listenLoop, "Net-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Update the message listener after construction if needed.
     * Safe to call from any thread thanks to the volatile field (FIX #3).
     */
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    /**
     * Send a message to the server (non-blocking).
     *
     * FIX #5 – guard against sending on a closed/errored connection.
     * PrintWriter never throws; it silently sets an error flag. We now
     * check both `running` and `out.checkError()` to detect failures.
     */
    public void send(String message) {
        if (!running || out == null) return;

        out.println(message);

        // FIX #5 – detect silent PrintWriter stream errors
        if (out.checkError()) {
            System.err.println("NetworkClient: send failed – stream error detected.");
            close();
        }
    }

    /** Continuously reads lines from the server and fires the listener. */
    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                final String msg = line.trim();
                Consumer<String> cb = messageListener;
                if (cb != null) cb.accept(msg);
            }
        } catch (IOException e) {
            if (running) {
                Consumer<String> cb = messageListener;
                if (cb != null) cb.accept("DISCONNECT:lost_connection");
            }
        } finally {
            // FIX #4 – always clean up and notify the listener when the loop exits,
            // whether the server disconnected cleanly (readLine → null) or with an error.
            if (running) {
                Consumer<String> cb = messageListener;
                if (cb != null) cb.accept("DISCONNECT:lost_connection");
            }
            close();
        }
    }

    /** Close the connection gracefully. */
    public void close() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
