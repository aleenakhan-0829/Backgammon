/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.yourpackage.backgammon.clienthandler;

import com.mycompany.backgammongame.BackgammonGame;
import java.net.Socket;
import java.io.*;

/**
 *
 * @author Aleena's PC
 */
    

public class Clienthandler implements Runnable {

    private final Socket socket;
    private final int playerNumber;
    private final BackgammonGame server;

    private PrintWriter out;
    private BufferedReader in;

    private volatile boolean running = true;

    public ClientHandler(Socket socket, int playerNumber, BackgammonGame server) {
        this.socket = socket;
        this.playerNumber = playerNumber;
        this.server = server;

        try {
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            log("Connected: " + socket.getInetAddress());

            // Send player identity immediately
            send("PLAYER_NUM:" + playerNumber);

        } catch (IOException e) {
            log("Initialization error: " + e.getMessage());
            running = false;
        }
    }

    @Override
    public void run() {

        if (!running) {
            close();
            return;
        }

        try {
            String line;

            while (running && (line = in.readLine()) != null) {

                line = line.trim();

                // Ignore empty messages
                if (line.isEmpty()) continue;

                log("Received -> " + line);

                // Basic protocol validation
                if (!isValidMessage(line)) {
                    send("ERROR:invalid_format");
                    continue;
                }

                // Forward to server (core logic)
                server.handleMessage(playerNumber, line);
            }

        } catch (IOException e) {
            log("Connection lost.");
        } finally {
            shutdown();
        }
    }

    // ---------------------------------------------------------------
    // Message Validation
    // ---------------------------------------------------------------

    private boolean isValidMessage(String msg) {

        // Expected commands from your BackgammonGame
        return msg.startsWith("READY") ||
               msg.startsWith("ROLL") ||
               msg.startsWith("MOVE") ||
               msg.startsWith("CHAT") ||
               msg.startsWith("RESIGN") ||
               msg.startsWith("REPLAY");
    }

    // ---------------------------------------------------------------
    // Sending
    // ---------------------------------------------------------------

    public void send(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
            log("Sent -> " + message);
        }
    }

    // ---------------------------------------------------------------
    // Shutdown
    // ---------------------------------------------------------------

    private void shutdown() {
        running = false;

        log("Disconnecting...");

        server.playerDisconnected(playerNumber);
        close();
    }

    private void close() {
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}

        if (out != null) out.close();

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ---------------------------------------------------------------
    // Logging
    // ---------------------------------------------------------------

    private void log(String msg) {
        System.out.println("[Player " + playerNumber + "] " + msg);
    }
}
    
