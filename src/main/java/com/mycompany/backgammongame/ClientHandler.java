/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import java.io.*;
import java.net.*;

/**
 * ClientHandler - Runs in its own thread.
 * Reads messages from one client and forwards them to the server.
 */
public class ClientHandler implements Runnable {  // FIX #1 – renamed Clienthandler → ClientHandler

    private final Socket socket;
    private final int playerNumber;
    private final BackgammonGame server;  // FIX #2 – was BackgammonServer, must be BackgammonGame

    private PrintWriter out;
    private BufferedReader in;

    // FIX #4 & #5 – track whether setup succeeded so run() and close() behave correctly
    private boolean initialized = false;

    public ClientHandler(Socket socket, int playerNumber, BackgammonGame server) {
        this.socket       = socket;
        this.playerNumber = playerNumber;
        this.server       = server;

        try {
            // Auto-flush enabled so every send() call flushes immediately
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())), true);
            in  = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Tell the client which player number they are
            out.println("PLAYER_NUM:" + playerNumber);

            initialized = true;  // FIX #4 & #5 – only true when streams are ready
        } catch (IOException e) {
            System.err.println("Handler setup error for player " + playerNumber + ": " + e.getMessage());
            close(); // clean up the socket immediately if setup fails
        }
    }

    @Override
    public void run() {
        // FIX #4 & #5 – skip the read loop entirely if setup failed;
        // avoids NullPointerException on `in` and a false playerDisconnected() call
        if (!initialized) {
            System.err.println("Player " + playerNumber + " handler was not initialized; skipping run.");
            return;
        }

        try {
            String line;
            while ((line = in.readLine()) != null) {
                server.handleMessage(playerNumber, line.trim());
            }
        } catch (IOException e) {
            System.out.println("Player " + playerNumber + " connection lost.");
        } finally {
            // Only notify the server of a real disconnect (not a failed setup)
            server.playerDisconnected(playerNumber);
            close();
        }
    }

    /** Send a message to this client. */
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /** FIX #3 – changed from private to public so BackgammonGame.closePreviousSession() can call it */
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}

    

