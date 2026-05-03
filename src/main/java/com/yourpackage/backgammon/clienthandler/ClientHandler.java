/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.yourpackage.backgammon.clienthandler;

import com.mycompany.backgammongame.BackgammonGame;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 *
 * @author Aleena's PC
 */
    public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int playerNumber;
    private final BackgammonGame server; 

    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, int playerNumber, BackgammonGame server) {
        this.socket = socket;
        this.playerNumber = playerNumber;
        this.server = server;

        try {
            // Auto-flush enabled
            out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())), true);

            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Inform client of player number
            out.println("PLAYER_NUM:" + playerNumber);

        } catch (IOException e) {
            System.err.println("Handler setup error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String line;

            while ((line = in.readLine()) != null) {
                server.handleMessage(playerNumber, line.trim());
            }

        } catch (IOException e) {
            System.out.println("Player " + playerNumber + " connection lost.");
        } finally {
            server.playerDisconnected(playerNumber);
            close();
        }
    }

    /** Send message to client */
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /** Close socket safely */
    private void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}