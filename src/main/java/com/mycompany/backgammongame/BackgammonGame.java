/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.backgammongame;

import com.yourpackage.backgammon.clienthandler.ClientHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

/**
 *
 * @author Aleena's PC
 */
public class BackgammonGame {

    // Port the server listens on
    private static final int SERVER_PORT = 10534;

    // Networking
    private ServerSocket gameserverSocket;

    // Two client handlers (one per player)
    private ClientHandler playerA;
    private ClientHandler playerB;

    // Shared game state
    private GameState boardState;

    // Lock object for synchronisation
    private final Object gamelock = new Object();

    // Game flow counters
    private int playersready = 0;
    private int playagaincount = 0;

    public static void main(String[] args) {
        System.out.println("=== Backgammon Server Starting ===");
        new BackgammonGame().start();
    }

    public void start() {
        try {
            gameserverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server listening on port " + SERVER_PORT);

            while (true) {
                System.out.println("\nWaiting for Player 1...");
                Socket s1 = gameserverSocket.accept();
                System.out.println("Player 1 connected: " + s1.getInetAddress());

                System.out.println("Waiting for Player 2...");
                Socket s2 = gameserverSocket.accept();
                System.out.println("Player 2 connected: " + s2.getInetAddress());

                boardState = new GameState();

                playerA = new ClientHandler(s1, 1, this);
                playerB = new ClientHandler(s2, 2, this);

                new Thread(playerA).start();
                new Thread(playerB).start();

                System.out.println("Game session started!");
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void handleMessage(int fromPlayer, String message) {
        if (message == null) return;

        synchronized (gamelock) {
            System.out.println("[P" + fromPlayer + "] -> " + message);

            String[] parts = message.split(":", 2);
            String command = parts[0];
            String data = (parts.length > 1) ? parts[1] : "";

            switch (command) {
                case "READY":
                    handleReady(fromPlayer);
                    break;
                case "ROLL":
                    handleRoll(fromPlayer);
                    break;
                case "MOVE":
                    handleMove(fromPlayer, data);
                    break;
                case "CHAT":
                    broadcast("CHAT:P" + fromPlayer + ": " + data);
                    break;
                case "RESIGN":
                    handleResign(fromPlayer);
                    break;
                case "REPLAY":
                    handleReplay(fromPlayer);
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }
    }

    private void handleReady(int player) {
        playersready++;

        if (playersready == 2) {
            playersready = 0;
            boardState.reset();

            broadcast("START:" + boardState.serialize());

            sendToPlayer(boardState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(boardState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    private void handleRoll(int player) {
        if (player != boardState.getCurrentPlayer()) {
            sendToPlayer(player, "ERROR:not_your_turn");
            return;
        }

        int[] dice = boardState.rollDice();
        broadcast("DICE:" + dice[0] + "," + dice[1]);

        if (boardState.hasValidMoves()) {
            sendToPlayer(player, "YOUR_TURN:move");
            sendToOpponent(player, "WAIT:opponent_moving");
        } else {
            broadcast("INFO:No valid moves for Player " + player + ". Skipping turn.");

            boardState.nextTurn();
            sendToPlayer(boardState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(boardState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    private void handleMove(int player, String data) {
        if (player != boardState.getCurrentPlayer()) {
            sendToPlayer(player, "ERROR:not_your_turn");
            return;
        }

        String[] coords = data.split(",");
        if (coords.length != 2) {
            sendToPlayer(player, "ERROR:invalid_move_format");
            return;
        }

        int from, to;
        try {
            from = Integer.parseInt(coords[0]);
            to = Integer.parseInt(coords[1]);
        } catch (NumberFormatException e) {
            sendToPlayer(player, "ERROR:invalid_numbers");
            return;
        }

        MoveResult result = boardState.applyMove(player, from, to);

        if (!result.isValid()) {
            sendToPlayer(player, "ERROR:illegal_move");
            return;
        }

        broadcast("BOARD:" + boardState.serialize());

        if (result.isGameOver()) {
            broadcast("GAMEOVER:" + player);
            return;
        }

        if (boardState.hasRemainingMoves()) {
            sendToPlayer(player, "YOUR_TURN:move");
            sendToOpponent(player, "WAIT:opponent_moving");
        } else {
            boardState.nextTurn();
            sendToPlayer(boardState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(boardState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    private void handleResign(int player) {
        int winner = (player == 1) ? 2 : 1;
        broadcast("GAMEOVER:" + winner);
    }

    private void handleReplay(int player) {
        playagaincount++;

        sendToOpponent(player, "INFO:Player " + player + " wants to replay.");

        if (playagaincount == 2) {
            playagaincount = 0;

            handleReady(1);
            handleReady(2);
        }
    }

    // ---------------- helpers ----------------

    public void sendToPlayer(int player, String message) {
        ClientHandler handler = (player == 1) ? playerA : playerB;
        if (handler != null) handler.send(message);
    }

    public void sendToOpponent(int player, String message) {
        ClientHandler handler = (player == 1) ? playerB : playerA;
        if (handler != null) handler.send(message);
    }

    public void broadcast(String message) {
        if (playerA != null) playerA.send(message);
        if (playerB != null) playerB.send(message);
    }

    public void playerDisconnected(int player) {
        System.out.println("Player " + player + " disconnected.");

        int opponent = (player == 1) ? 2 : 1;
        sendToPlayer(opponent, "DISCONNECT:opponent_left");
    }
}