/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import java.util.*;

/**
 * ClientGameState - A local copy of the board state on the client.
 * Updated by deserialising messages from the server.
 * Does NOT enforce rules (the server is authoritative).
 */
public class ClientGameState {

    // Same layout as server GameState:
    // 0-23 = points, 24-25 = bars (P1=24, P2=25), 26-27 = bear-off (P1=26, P2=27)
    private int[] board = new int[28];

    private int currentPlayer = 1;
    private int[] dice = {0, 0};

    // Which player THIS client is (set once when PLAYER_NUM message arrives)
    private int myPlayerNumber = 0;

    // Dice values still available for this turn
    private List<Integer> movesLeft = new ArrayList<>();

    // ---------------------------------------------------------------
    // Deserialisation
    // ---------------------------------------------------------------

    /**
     * Parse the serialised board string sent by the server.
     * Format: board(28 ints)|currentPlayer|die1,die2|movesLeft(csv)
     *
     * FIX #2 – now also restores movesLeft from section 4 so the
     *           client stays in sync with the server after every BOARD update.
     * FIX #3 – wrapped in try/catch so a malformed network message cannot
     *           crash the client thread.
     */
    public void deserialize(String data) {
        try {
            String[] sections = data.split("\\|");
            if (sections.length < 3) return;

            // Section 0: board
            String[] cells = sections[0].split(",");
            for (int i = 0; i < 28 && i < cells.length; i++) {
                board[i] = Integer.parseInt(cells[i]);
            }

            // Section 1: currentPlayer
            currentPlayer = Integer.parseInt(sections[1]);

            // Section 2: dice
            String[] d = sections[2].split(",");
            dice[0] = Integer.parseInt(d[0]);
            dice[1] = Integer.parseInt(d[1]);

            // FIX #2 – Section 3: movesLeft (present in updated server format)
            movesLeft.clear();
            if (sections.length > 3 && !sections[3].isEmpty()) {
                for (String val : sections[3].split(",")) {
                    movesLeft.add(Integer.parseInt(val.trim()));
                }
            }

        } catch (NumberFormatException e) {
            // FIX #3 – log and ignore; don't let bad data crash the client thread
            System.err.println("ClientGameState deserialize error: " + e.getMessage());
        }
    }

    /** Called when the server sends a DICE message. */
    public void setDice(int d1, int d2) {
        dice[0] = d1;
        dice[1] = d2;
        movesLeft.clear();
        if (d1 == d2) {
            for (int i = 0; i < 4; i++) movesLeft.add(d1);
        } else {
            movesLeft.add(d1);
            movesLeft.add(d2);
        }
    }

    /** Mark one die value as used. */
    public void useMove(int dieValue) {
        movesLeft.remove(Integer.valueOf(dieValue));
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public int[]         getBoard()         { return board; }
    public int           getCurrentPlayer() { return currentPlayer; }
    public int[]         getDice()          { return dice; }
    public List<Integer> getMovesLeft()     { return Collections.unmodifiableList(movesLeft); }
    public int           getMyPlayerNumber(){ return myPlayerNumber; }

    /**
     * FIX #4 – validate that only 1 or 2 are accepted; silently ignoring
     *           bad values caused all "is it my turn?" checks to misbehave.
     */
    public void setMyPlayerNumber(int n) {
        if (n == 1 || n == 2) {
            this.myPlayerNumber = n;
        } else {
            System.err.println("ClientGameState: invalid player number " + n + " (must be 1 or 2)");
        }
    }

    /** Convenience: how many checkers does player p have at index i? */
    public int countAt(int player, int idx) {
        int val = board[idx];
        if (player == 1) return Math.max(0, val);
        return Math.max(0, -val);
    }

    /** Returns 1 if player 1 owns point i, 2 if player 2, 0 if empty. */
    public int ownerAt(int idx) {
        if (board[idx] > 0) return 1;
        if (board[idx] < 0) return 2;
        return 0;
    }

    public int getBarIndex(int player)     { return (player == 1) ? 24 : 25; }
    public int getBearOffIndex(int player) { return (player == 1) ? 26 : 27; }

} // FIX #1 – removed the extra stray `}` that caused a compile error
