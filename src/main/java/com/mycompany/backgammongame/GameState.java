/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import java.util.*;

public class GameState {

    // Board: 0-23 points, 24-25 bars (P1=24, P2=25), 26-27 bear-off (P1=26, P2=27)
    private int[] board = new int[28];

    private int currentPlayer = 1;
    private int[] dice = new int[2];
    private List<Integer> movesLeft = new ArrayList<>();

    private static final Random RANDOM = new Random();

    // ---------------------------------------------------------------
    // Init
    // ---------------------------------------------------------------

    public GameState() {
        reset();
    }

    public void reset() {
        Arrays.fill(board, 0);

        // Standard Backgammon Setup
        // P1 moves 0→23, home board = 18-23
        board[0]  =  2;
        board[11] =  5;
        board[16] =  3;
        board[18] =  5;

        // P2 moves 23→0, home board = 0-5
        board[23] = -2;
        board[12] = -5;
        board[7]  = -3;
        board[5]  = -5;

        currentPlayer = 1;
        movesLeft.clear();
        Arrays.fill(dice, 0);
    }

    // ---------------------------------------------------------------
    // Dice
    // ---------------------------------------------------------------

    public int[] rollDice() {
        dice[0] = RANDOM.nextInt(6) + 1;
        dice[1] = RANDOM.nextInt(6) + 1;

        movesLeft.clear();

        if (dice[0] == dice[1]) {
            for (int i = 0; i < 4; i++) movesLeft.add(dice[0]);
        } else {
            movesLeft.add(dice[0]);
            movesLeft.add(dice[1]);
        }

        return dice;
    }

    // ---------------------------------------------------------------
    // Move
    // ---------------------------------------------------------------

    public MoveResult applyMove(int player, int from, int to) {

        if (from < 0 || from > 27 || to < 0 || to > 27) {
            return new MoveResult(false, false);
        }

        if (!isValidMove(player, from, to)) {
            return new MoveResult(false, false);
        }

        int sign = (player == 1) ? 1 : -1;

        // Remove checker from source (bar or regular point)
        board[from] -= sign;

        // Hit opponent if landing on a blot (single enemy checker)
        if (to >= 0 && to <= 23 && Math.abs(board[to]) == 1 && board[to] * sign < 0) {
            int opponent   = 3 - player;
            int opBarIdx   = getBarIndex(opponent);
            board[to]      = 0;
            // FIX #4 – removed the redundant if/else; opponent sign is always -(sign)
            board[opBarIdx] -= sign;
        }

        // FIX #4 – removed the dead if/else; both branches were identical
        board[to] += sign;

        // Use die
        int dieValue = calculateDieValue(player, from, to);
        useDie(dieValue);

        boolean gameOver = checkWin(player);

        return new MoveResult(true, gameOver);
    }

    public boolean isValidMove(int player, int from, int to) {

        int sign   = (player == 1) ? 1 : -1;
        int barIdx = getBarIndex(player);

        // Must enter from bar first
        if (board[barIdx] * sign > 0 && from != barIdx) return false;

        // Validate source
        if (from == barIdx) {
            if (board[barIdx] * sign <= 0) return false;
        } else {
            if (from < 0 || from > 23)     return false;
            if (board[from] * sign <= 0)   return false;
        }

        // Destination limits
        if (to < 0 || to > 27) return false;

        int dieNeeded = calculateDieValue(player, from, to);

        // Die must exist OR special over-roll bearing off
        if (!movesLeft.contains(dieNeeded)) {

            if (to != getBearOffIndex(player)) return false;
            if (!isBearingOffAllowed(player))  return false;

            boolean canUseLarger = movesLeft.stream().anyMatch(d -> d > dieNeeded);
            if (!canUseLarger || !noCheckersOutsideHome(player, from)) return false;
        }

        // Blocked point (2+ opponent checkers)
        if (to >= 0 && to <= 23 && board[to] * sign < -1) return false;

        // Bearing off rule
        if (to == getBearOffIndex(player) && !isBearingOffAllowed(player)) return false;

        return true;
    }

    public boolean hasValidMoves() {
        int player = currentPlayer;

        List<Integer> sources = new ArrayList<>();
        sources.add(getBarIndex(player));
        for (int i = 0; i <= 23; i++) sources.add(i);

        for (int from : sources) {
            for (int to = 0; to <= 27; to++) {
                if (isValidMove(player, from, to)) return true;
            }
        }
        return false;
    }

    public boolean hasRemainingMoves() {
        return !movesLeft.isEmpty() && hasValidMoves();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * FIX #1 – bar entry formula was swapped between P1 and P2.
     *   P1 moves 0→23: enters opponent's home (points 0-5), die = to + 1
     *   P2 moves 23→0: enters opponent's home (points 18-23), die = 24 - to
     *
     * FIX #2 – bear-off formula used Math.abs(bearOffIndex - from) which
     *   gives completely wrong values. Correct formulas:
     *   P1 bearing off from `from` (18-23): die = 24 - from
     *   P2 bearing off from `from` (0-5):   die = from + 1
     */
    private int calculateDieValue(int player, int from, int to) {
        int barIdx     = getBarIndex(player);
        int bearOffIdx = getBearOffIndex(player);

        if (from == barIdx) {
            // Entering from bar
            // FIX #1 – was: (player==1) ? (24-to) : (to+1)  ← swapped!
            return (player == 1) ? (to + 1) : (24 - to);
        }

        if (to == bearOffIdx) {
            // Bearing off
            // FIX #2 – was: Math.abs(to - from) which used 26 or 27 as `to`
            return (player == 1) ? (24 - from) : (from + 1);
        }

        // Regular move: distance between points (always positive)
        return Math.abs(to - from);
    }

    private boolean isBearingOffAllowed(int player) {
        int sign  = (player == 1) ? 1 : -1;
        int start = (player == 1) ? 18 : 0;
        int end   = (player == 1) ? 23 : 5;

        int total = 0;

        for (int i = start; i <= end; i++) {
            if (board[i] * sign > 0) total += board[i] * sign;
        }

        total += board[getBearOffIndex(player)] * sign;

        // If any checkers are on the bar or outside home, total < 15
        return total == 15;
    }

    /**
     * FIX #3 – was missing a bar check.
     * If the player still has a checker on the bar, they cannot use an
     * oversized die to bear off, so this must return false.
     */
    private boolean noCheckersOutsideHome(int player, int from) {
        int sign   = (player == 1) ? 1 : -1;
        int start  = (player == 1) ? 18 : 0;
        int end    = (player == 1) ? 23 : 5;
        int barIdx = getBarIndex(player);

        // FIX #3 – check the bar explicitly (not covered by the 0-23 loop)
        if (board[barIdx] * sign > 0) return false;

        for (int i = 0; i <= 23; i++) {
            if ((i < start || i > end) && i != from) {
                if (board[i] * sign > 0) return false;
            }
        }
        return true;
    }

    private boolean checkWin(int player) {
        int sign = (player == 1) ? 1 : -1;
        return board[getBearOffIndex(player)] * sign >= 15;
    }

    public void nextTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        movesLeft.clear();
    }

    public int getBarIndex(int player)     { return (player == 1) ? 24 : 25; }
    public int getBearOffIndex(int player) { return (player == 1) ? 26 : 27; }
    public int getCurrentPlayer()         { return currentPlayer; }

    private void useDie(int value) {
        movesLeft.remove(Integer.valueOf(value));
    }

    // ---------------------------------------------------------------
    // Serialization
    // ---------------------------------------------------------------

    public String serialize() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 28; i++) {
            sb.append(board[i]);
            if (i < 27) sb.append(",");
        }

        sb.append("|").append(currentPlayer);
        sb.append("|").append(dice[0]).append(",").append(dice[1]);

        // FIX #5 – also serialize movesLeft so it can be restored on deserialize
        sb.append("|");
        for (int i = 0; i < movesLeft.size(); i++) {
            sb.append(movesLeft.get(i));
            if (i < movesLeft.size() - 1) sb.append(",");
        }

        return sb.toString();
    }

    /**
     * FIX #5 – was not restoring movesLeft.
     * Deserializing mid-game left movesLeft empty, making it impossible
     * for the active player to make any moves after a state restore.
     */
    public void deserialize(String data) {
        try {
            String[] sections = data.split("\\|");

            // Section 0: board
            String[] cells = sections[0].split(",");
            for (int i = 0; i < 28; i++) {
                board[i] = Integer.parseInt(cells[i]);
            }

            // Section 1: currentPlayer
            currentPlayer = Integer.parseInt(sections[1]);

            // Section 2: dice
            String[] d = sections[2].split(",");
            dice[0] = Integer.parseInt(d[0]);
            dice[1] = Integer.parseInt(d[1]);

            // FIX #5 – Section 3: movesLeft (may be absent in legacy data)
            movesLeft.clear();
            if (sections.length > 3 && !sections[3].isEmpty()) {
                for (String val : sections[3].split(",")) {
                    movesLeft.add(Integer.parseInt(val));
                }
            }

        } catch (Exception e) {
            System.err.println("Deserialize error: " + e.getMessage());
        }
    }

    public int[] getBoard() {
        return board;
    }
}
