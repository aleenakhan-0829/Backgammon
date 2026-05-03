/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;


/**
 *
 * @author Aleena's PC
 */

public class NetworkClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Callback invoked on the calling thread whenever a message arrives
    private Consumer<String> messageListener;

    private Thread listenerThread;
    private volatile boolean running = false;

    /**
     * Connect to the server. Blocks until the connection is established
     * (or throws IOException on failure).
     */
    public NetworkClient(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000); // 5-second timeout
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        // Start background listener thread
        listenerThread = new Thread(this::listenLoop, "Net-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Register the callback that will receive every incoming server message. */
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
    }

    /** Send a message to the server (non-blocking). */
    public void send(String message) {
        if (out != null) out.println(message);
    }

    /** Continuously reads lines from the server and fires the listener. */
    private void listenLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                final String msg = line.trim();
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            }
        } catch (IOException e) {
            if (running && messageListener != null) {
                messageListener.accept("DISCONNECT:lost_connection");
            }
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

    

