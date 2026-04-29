/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.backgammongame;

import java.net.Socket;

/**
 *
 * @author Aleena's PC
 */
public class BackgammonGame {
    // Port the server listens on
    private static final int PORT = 5555;

    // Two client handlers (one per player)
    private ClientHandler player1;
    private ClientHandler player2;

    // Shared game state (single instance, both handlers reference it)
    private GameState gameState;

    // Lock object for synchronisation
    private final Object lock = new Object();

    public static void main(String[] args) {
        System.out.println("=== Backgammon Server Starting ===");
        new BackgammonServer().start();
    }

    public void start() {
        try {
            System.out.println("Server listening on port " + PORT);

            // Keep accepting new game sessions indefinitely
            while (true) {
                System.out.println("\nWaiting for Player 1...");
                Socket s1 = serverSocket.accept();
                System.out.println("Player 1 connected: " + s1.getInetAddress());

                System.out.println("Waiting for Player 2...");
                Socket s2 = serverSocket.accept();
                System.out.println("Player 2 connected: " + s2.getInetAddress());

                // Fresh game state for each session
                gameState = new GameState();

                player1 = new ClientHandler(s1, 1, this);
                player2 = new ClientHandler(s2, 2, this);

                // Start both handler threads
                new Thread(player1).start();
                new Thread(player2).start();

                System.out.println("Game session started!");
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Called by a ClientHandler when it receives a message.
     * Routes the message to the correct destination.
     */
    public void handleMessage(int fromPlayer, String message) {
        synchronized (lock) {
            System.out.println("[P" + fromPlayer + "] -> " + message);

            String[] parts = message.split(":", 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

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

    // ---------------------------------------------------------------
    // Game-flow handlers
    // ---------------------------------------------------------------

    private int readyCount = 0;
    private int replayCount = 0;

    /** Both players must send READY before the game begins. */
    private void handleReady(int player) {
        readyCount++;
        if (readyCount == 2) {
            readyCount = 0;
            gameState.reset();
            broadcast("START:" + gameState.serialize());
            sendToPlayer(gameState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(gameState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    /** Player requests a dice roll. */
    private void handleRoll(int player) {
        if (player != gameState.getCurrentPlayer()) {
            sendToPlayer(player, "ERROR:not_your_turn");
            return;
        }
        int[] dice = gameState.rollDice();
        broadcast("DICE:" + dice[0] + "," + dice[1]);

        // Check if the current player has any valid moves
        if (gameState.hasValidMoves()) {
            sendToPlayer(player, "YOUR_TURN:move");
            sendToOpponent(player, "WAIT:opponent_moving");
        } else {
            // No valid moves – skip turn
            broadcast("INFO:No valid moves for Player " + player + ". Skipping turn.");
            gameState.nextTurn();
            sendToPlayer(gameState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(gameState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    /** Player sends a move: "from,to" */
    private void handleMove(int player, String data) {
        if (player != gameState.getCurrentPlayer()) {
            sendToPlayer(player, "ERROR:not_your_turn");
            return;
        }

        String[] coords = data.split(",");
        if (coords.length != 2) {
            sendToPlayer(player, "ERROR:invalid_move_format");
            return;
        }

        int from = Integer.parseInt(coords[0]);
        int to   = Integer.parseInt(coords[1]);

        MoveResult result = gameState.applyMove(player, from, to);
        if (!result.isValid()) {
            sendToPlayer(player, "ERROR:illegal_move");
            return;
        }

        // Broadcast the updated board to both players
        broadcast("BOARD:" + gameState.serialize());

        if (result.isGameOver()) {
            broadcast("GAMEOVER:" + player);
            return;
        }

        if (gameState.hasRemainingMoves()) {
            // Same player continues using remaining dice
            sendToPlayer(player, "YOUR_TURN:move");
            sendToOpponent(player, "WAIT:opponent_moving");
        } else {
            gameState.nextTurn();
            sendToPlayer(gameState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(gameState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

    private void handleResign(int player) {
        int winner = (player == 1) ? 2 : 1;
        broadcast("GAMEOVER:" + winner);
    }

    private void handleReplay(int player) {
        replayCount++;
        sendToOpponent(player, "INFO:Player " + player + " wants to replay.");
        if (replayCount == 2) {
            replayCount = 0;
            handleReady(0); // re-use ready flow
        }
    }

    // ---------------------------------------------------------------
    // Messaging helpers
    // ---------------------------------------------------------------

    public void sendToPlayer(int player, String message) {
        ClientHandler handler = (player == 1) ? player1 : player2;
        if (handler != null) handler.send(message);
    }

    public void sendToOpponent(int player, String message) {
        ClientHandler handler = (player == 1) ? player2 : player1;
        if (handler != null) handler.send(message);
    }

    public void broadcast(String message) {
        if (player1 != null) player1.send(message);
        if (player2 != null) player2.send(message);
    }

    /** Called when a client disconnects. */
    public void playerDisconnected(int player) {
        System.out.println("Player " + player + " disconnected.");
        int opponent = (player == 1) ? 2 : 1;
        sendToPlayer(opponent, "DISCONNECT:opponent_left");
    }
}

