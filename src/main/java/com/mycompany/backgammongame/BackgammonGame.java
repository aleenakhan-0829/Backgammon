/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.backgammongame;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class BackgammonGame {

    private static final int SERVER_PORT = 10534;

    private ServerSocket gameServerSocket;

    private ClientHandler playerA;
    private ClientHandler playerB;

    private GameState boardState;

    private final Object gameLock = new Object();

    private int playersReady   = 0;
    private int playAgainCount = 0;
    private boolean diceRolled = false; // FIX #6 – guard against duplicate ROLL

    public static void main(String[] args) {
        System.out.println("=== Backgammon Server Starting ===");
        new BackgammonGame().start();
    }

    public void start() {
        try {
            gameServerSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server listening on port " + SERVER_PORT);

            while (true) {
                System.out.println("\nWaiting for Player 1...");
                Socket s1 = gameServerSocket.accept();
                System.out.println("Player 1 connected: " + s1.getInetAddress());

                System.out.println("Waiting for Player 2...");
                Socket s2 = gameServerSocket.accept();
                System.out.println("Player 2 connected: " + s2.getInetAddress());

                // FIX #1 & #2 – close old sockets and assign new handlers inside
                // synchronized block so handler threads see consistent state.
                synchronized (gameLock) {
                    // FIX #8 – close previous sockets to avoid resource leak
                    closePreviousSession();

                    boardState     = new GameState();
                    playersReady   = 0;   // FIX #4 – reset counters for new session
                    playAgainCount = 0;
                    diceRolled     = false;

                    playerA = new ClientHandler(s1, 1, this);
                    playerB = new ClientHandler(s2, 2, this);
                }

                new Thread(playerA).start();
                new Thread(playerB).start();

                System.out.println("Game session started!");
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // FIX #8 – helper that gracefully closes the previous session's handlers
    private void closePreviousSession() {
        if (playerA != null) { playerA.close(); playerA = null; }
        if (playerB != null) { playerB.close(); playerB = null; }
    }

    public void handleMessage(int fromPlayer, String message) {
        if (message == null) return;

        synchronized (gameLock) {
            System.out.println("[P" + fromPlayer + "] -> " + message);

            // FIX #5 – guard against messages arriving before the game is ready
            if (boardState == null) {
                sendToPlayer(fromPlayer, "ERROR:game_not_started");
                return;
            }

            String[] parts   = message.split(":", 2);
            String   command = parts[0];
            String   data    = (parts.length > 1) ? parts[1] : "";

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
        playersReady++;

        if (playersReady == 2) {
            playersReady = 0;
            diceRolled   = false;

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

        // FIX #6 – prevent rolling dice more than once per turn
        if (diceRolled) {
            sendToPlayer(player, "ERROR:already_rolled");
            return;
        }

        diceRolled = true;
        int[] dice = boardState.rollDice();
        broadcast("DICE:" + dice[0] + "," + dice[1]);

        if (boardState.hasValidMoves()) {
            sendToPlayer(player, "YOUR_TURN:move");
            sendToOpponent(player, "WAIT:opponent_moving");
        } else {
            broadcast("INFO:No valid moves for Player " + player);
            advanceTurn();
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
            to   = Integer.parseInt(coords[1]);
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
            advanceTurn();
        }
    }

    // FIX #3 – extracted shared turn-advance logic used by handleRoll and handleMove
    private void advanceTurn() {
        diceRolled = false;
        boardState.nextTurn();
        sendToPlayer(boardState.getCurrentPlayer(), "YOUR_TURN:roll");
        sendToOpponent(boardState.getCurrentPlayer(), "WAIT:opponent_rolling");
    }

    private void handleResign(int player) {
        int winner = (player == 1) ? 2 : 1;
        broadcast("GAMEOVER:" + winner);
    }

    private void handleReplay(int player) {
        playAgainCount++;
        sendToOpponent(player, "INFO:Player " + player + " wants to replay.");

        // FIX #3 – restart the game directly instead of calling handleReady() twice,
        // which previously relied on playersReady side-effects and was fragile.
        if (playAgainCount == 2) {
            playAgainCount = 0;
            diceRolled     = false;

            boardState.reset();
            broadcast("START:" + boardState.serialize());

            sendToPlayer(boardState.getCurrentPlayer(), "YOUR_TURN:roll");
            sendToOpponent(boardState.getCurrentPlayer(), "WAIT:opponent_rolling");
        }
    }

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

    // FIX #7 – clean up all session state so it doesn't bleed into the next game
    public void playerDisconnected(int player) {
        int opponent = (player == 1) ? 2 : 1;
        sendToPlayer(opponent, "DISCONNECT:opponent_left");

        // Null out the disconnected handler so broadcast() skips it safely
        if (player == 1) { playerA = null; }
        else             { playerB = null; }

        // Reset session counters so a reconnect starts clean
        playersReady   = 0;
        playAgainCount = 0;
        diceRolled     = false;
    }
}