/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author Aleena's PC
 */

    
    

/**
 * BoardPanel - Renders the backgammon board and handles player input.
 *
 * Visual layout:
 *   Points 13-24 are drawn along the top (left to right)
 *   Points 12-1  are drawn along the bottom (left to right)
 *   Bar area in the centre
 *   Bear-off trays on the right side
 *
 * Click behaviour:
 *   1st click  = select a source point (highlights valid destinations)
 *   2nd click  = confirm destination → fires moveCallback(from, to)
 */
public class BoardPanel extends JPanel {

    // ---- Colours ----
    private static final Color BOARD_BG    = new Color(139, 90,  43);
    private static final Color BORDER_CLR  = new Color( 80, 45,  15);
    private static final Color TRI_RED     = new Color(180, 40,  30);
    private static final Color TRI_CREAM   = new Color(245, 230, 190);
    private static final Color BAR_CLR     = new Color( 70, 40,  10);
    private static final Color P1_CHECKER  = new Color(240, 230, 210);  // White/cream
    private static final Color P2_CHECKER  = new Color( 40,  30,  20);  // Dark brown/black
    private static final Color SELECTED    = new Color(255, 220,   0, 180);
    private static final Color VALID_DEST  = new Color( 50, 200,  80, 140);
    private static final Color DICE_BG     = new Color(245, 245, 245);
    private static final Color CREAM = new Color(255, 253, 208);

    private ClientGameState state;

    // Interaction state
    private int selectedPoint = -1;       // -1 = nothing selected
    private List<Integer> validDests = new ArrayList<>();
    private BiConsumer<Integer, Integer> moveCallback; // (from, to)

    // Geometry (computed in paintComponent to stay responsive)
    private int boardX, boardY, boardW, boardH;
    private int pointW;   // width of one point column
    private int barX, barW;

    // Whether the local player can interact
    private boolean myTurn = false;

    public BoardPanel(ClientGameState state) {
        this.state = state;
        setBackground(BORDER_CLR);
        setPreferredSize(new Dimension(750, 480));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
    }

    public void setMoveCallback(BiConsumer<Integer, Integer> cb) { this.moveCallback = cb; }
    public void setMyTurn(boolean b) { myTurn = b; clearSelection(); }

    // ---------------------------------------------------------------
    // Painting
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        computeGeometry();
        drawBoard(g2);
        drawTriangles(g2);
        drawBar(g2);
        drawCheckers(g2);
        drawBearOffTrays(g2);
        drawDice(g2);
        drawSelection(g2);
        drawLabels(g2);
    }

    private void computeGeometry() {
        int margin = 20;
        boardX = margin;
        boardY = margin;
        boardW = getWidth()  - 2 * margin - 60; // 60px for bear-off tray
        boardH = getHeight() - 2 * margin;
        barW   = boardW / 14;
        barX   = boardX + 6 * (boardW / 13);
        pointW = (boardW - barW) / 12;
    }

    private void drawBoard(Graphics2D g2) {
        g2.setColor(BOARD_BG);
        g2.fillRect(boardX, boardY, boardW, boardH);
        g2.setColor(BORDER_CLR);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(boardX, boardY, boardW, boardH);
    }

    /**
     * Draw the 24 triangles.
     * Points 13-24 → top row   (indices in board array: 12-23 left to right)
     * Points 12-1  → bottom row (indices in board array: 11-0  left to right)
     */
    private void drawTriangles(Graphics2D g2) {
        int midY = boardY + boardH / 2;

        for (int col = 0; col < 12; col++) {
            int pointIndex = getPointIndex(col, true);  // top row
            int x          = getPointX(col);
            Color c        = (col % 2 == 0) ? TRI_RED : TRI_CREAM;

            // Top triangle (points down toward middle)
            int[] xs = {x, x + pointW, x + pointW / 2};
            int[] ys = {boardY, boardY, midY - 4};
            g2.setColor(c);
            g2.fillPolygon(xs, ys, 3);

            // Highlight valid destination
            if (validDests.contains(pointIndex)) {
                g2.setColor(VALID_DEST);
                g2.fillPolygon(xs, ys, 3);
            }
            g2.setColor(BORDER_CLR);
            g2.drawPolygon(xs, ys, 3);

            // Bottom triangle (points up toward middle)
            int pointIndexB = getPointIndex(col, false);
            int[] xsB = {x, x + pointW, x + pointW / 2};
            int[] ysB = {boardY + boardH, boardY + boardH, midY + 4};
            Color cB = (col % 2 == 0) ? TRI_RED : TRI_CREAM;
            g2.setColor(cB);
            g2.fillPolygon(xsB, ysB, 3);
            if (validDests.contains(pointIndexB)) {
                g2.setColor(VALID_DEST);
                g2.fillPolygon(xsB, ysB, 3);
            }
            g2.setColor(BORDER_CLR);
            g2.drawPolygon(xsB, ysB, 3);
        }
    }

    private void drawBar(Graphics2D g2) {
        g2.setColor(BAR_CLR);
        g2.fillRect(barX, boardY, barW, boardH);

        // Checkers on bar
        drawBarCheckers(g2, 1);
        drawBarCheckers(g2, 2);
    }

    private void drawBarCheckers(Graphics2D g2, int player) {
        int count = Math.abs(state.getBoard()[state.getBarIndex(player)]);
        if (count == 0) return;

        int cx = barX + barW / 2;
        int r  = Math.min(barW / 2 - 2, 14);
        int startY = (player == 1) ? boardY + boardH / 2 + r + 4 : boardY + r + 4;
        int dir    = (player == 1) ? 1 : 1; // stack downward for P1, upward for P2
        if (player == 2) startY = boardY + boardH / 2 - r - 4;

        for (int i = 0; i < Math.min(count, 5); i++) {
            int cy = (player == 1) ? startY + i * (r * 2 + 2) : startY - i * (r * 2 + 2);
            paintChecker(g2, player, cx, cy, r);
        }
        if (count > 5) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("+" + (count - 5), cx - 6,
                    (player == 1) ? startY + 5 * (r * 2 + 2) : startY - 5 * (r * 2 + 2));
        }
    }

    private void drawCheckers(Graphics2D g2) {
        int[] board = state.getBoard();
        int midY    = boardY + boardH / 2;

        for (int col = 0; col < 12; col++) {
            // Top row
            int idxTop = getPointIndex(col, true);
            int x      = getPointX(col) + pointW / 2;
            int r      = Math.min(pointW / 2 - 2, 16);
            int cnt    = Math.abs(board[idxTop]);
            int owner  = state.ownerAt(idxTop);
            if (owner != 0) {
                for (int i = 0; i < Math.min(cnt, 5); i++) {
                    int cy = boardY + r + 3 + i * (r * 2 + 2);
                    paintChecker(g2, owner, x, cy, r);
                }
                if (cnt > 5) drawOverflow(g2, x, boardY + 5 * (r * 2 + 2) + r, cnt - 5);
            }

            // Bottom row
            int idxBot = getPointIndex(col, false);
            int cntB   = Math.abs(board[idxBot]);
            int ownerB = state.ownerAt(idxBot);
            if (ownerB != 0) {
                for (int i = 0; i < Math.min(cntB, 5); i++) {
                    int cy = boardY + boardH - r - 3 - i * (r * 2 + 2);
                    paintChecker(g2, ownerB, x, cy, r);
                }
                if (cntB > 5) drawOverflow(g2, x, boardY + boardH - 5 * (r * 2 + 2) - r, cntB - 5);
            }
        }
    }

    /** Draw a single checker circle. */
    private void paintChecker(Graphics2D g2, int player, int cx, int cy, int r) {
        Color base  = (player == 1) ? P1_CHECKER : P2_CHECKER;
        Color light = base.brighter();
        Color dark  = base.darker();

        // Shadow
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(cx - r + 2, cy - r + 2, r * 2, r * 2);

        // Body gradient effect (manual)
        g2.setColor(base);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Highlight
        g2.setColor(light);
        g2.fillOval(cx - r / 2, cy - r + 2, r, r / 2);

        // Border
        g2.setColor(dark);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);

        // If selected
        if (selectedPoint != -1) {
            int selIdx = boardIndexForColRow(selectedPoint);
            // (highlight drawn separately)
        }
    }

    private void drawOverflow(Graphics2D g2, int x, int y, int extra) {
        g2.setColor(Color.YELLOW);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("+" + extra, x - 6, y);
    }

    private void drawBearOffTrays(Graphics2D g2) {
        int tx = boardX + boardW + 5;
        int tw = 50;
        int th = boardH / 2 - 4;

        // P1 tray (bottom half, white)
        g2.setColor(new Color(200, 180, 140));
        g2.fillRect(tx, boardY + boardH / 2 + 4, tw, th);
        g2.setColor(BORDER_CLR);
        g2.drawRect(tx, boardY + boardH / 2 + 4, tw, th);

        // P2 tray (top half, dark)
        g2.setColor(new Color(60, 40, 20));
        g2.fillRect(tx, boardY, tw, th);
        g2.setColor(BORDER_CLR);
        g2.drawRect(tx, boardY, tw, th);

        int p1Off = Math.abs(state.getBoard()[state.getBearOffIndex(1)]);
        int p2Off = Math.abs(state.getBoard()[state.getBearOffIndex(2)]);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(P2_CHECKER);
        g2.drawString("P1", tx + 15, boardY + boardH / 2 + 18);
        g2.drawString("" + p1Off, tx + 18, boardY + boardH / 2 + 32);

        g2.setColor(P1_CHECKER);
        g2.drawString("P2", tx + 15, boardY + 16);
        g2.drawString("" + p2Off, tx + 18, boardY + 30);
    }

    private void drawDice(Graphics2D g2) {
        int[] dice = state.getDice();
        if (dice[0] == 0) return;

        int dSize = 36;
        int dY    = boardY + boardH / 2 - dSize / 2;
        int d1X   = barX + barW / 2 - dSize - 4;
        int d2X   = barX + barW / 2 + 4;

        drawDie(g2, dice[0], d1X, dY, dSize);
        drawDie(g2, dice[1], d2X, dY, dSize);
    }

    private void drawDie(Graphics2D g2, int value, int x, int y, int size) {
        g2.setColor(DICE_BG);
        g2.fillRoundRect(x, y, size, size, 6, 6);
        g2.setColor(BORDER_CLR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, size, size, 6, 6);

        g2.setColor(new Color(30, 10, 0));
        int[][] dots = getDotPositions(value, x, y, size);
        for (int[] dot : dots) {
            g2.fillOval(dot[0] - 3, dot[1] - 3, 6, 6);
        }
    }

    private int[][] getDotPositions(int v, int x, int y, int s) {
        int c = x + s / 2, m = y + s / 2;
        int o = s / 4;
        switch (v) {
            case 1: return new int[][]{{c, m}};
            case 2: return new int[][]{{x+o, y+o}, {x+s-o, y+s-o}};
            case 3: return new int[][]{{x+o, y+o}, {c, m}, {x+s-o, y+s-o}};
            case 4: return new int[][]{{x+o,y+o},{x+s-o,y+o},{x+o,y+s-o},{x+s-o,y+s-o}};
            case 5: return new int[][]{{x+o,y+o},{x+s-o,y+o},{c,m},{x+o,y+s-o},{x+s-o,y+s-o}};
            case 6: return new int[][]{{x+o,y+o},{x+s-o,y+o},{x+o,m},{x+s-o,m},{x+o,y+s-o},{x+s-o,y+s-o}};
            default: return new int[][]{};
        }
    }

    private void drawSelection(Graphics2D g2) {
        if (selectedPoint < 0) return;
        // Highlight ring around selected point column
        int col  = colForBoardIndex(selectedPoint);
        boolean top = selectedPoint >= 12;
        int x   = getPointX(col);
        int h   = boardH / 2;
        int yy  = top ? boardY : boardY + h;

        g2.setColor(SELECTED);
        g2.fillRect(x, yy, pointW, h);
    }

    private void drawLabels(Graphics2D g2) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.setColor(CREAM);
        // Top labels: 13 -> 24
        for (int col = 0; col < 12; col++) {
            int idx   = getPointIndex(col, true) + 1; // human-readable 1-24
            int x     = getPointX(col) + pointW / 2 - 6;
            g2.drawString(String.valueOf(idx), x, boardY + boardH + 14);
        }
        // Bottom labels: 12 -> 1
        for (int col = 0; col < 12; col++) {
            int idx = getPointIndex(col, false) + 1;
            int x   = getPointX(col) + pointW / 2 - 6;
            g2.drawString(String.valueOf(idx), x, boardY - 4);
        }
    }

    // ---------------------------------------------------------------
    // Interaction
    // ---------------------------------------------------------------

    private void handleClick(int mx, int my) {
        if (!myTurn) return;

        int clickedPoint = pointAt(mx, my);
        if (clickedPoint < 0) return;

        if (selectedPoint < 0) {
            // First click: select source
            if (state.ownerAt(clickedPoint) == state.getMyPlayerNumber() ||
                (clickedPoint == state.getBarIndex(state.getMyPlayerNumber()))) {
                selectedPoint = clickedPoint;
                validDests    = computeValidDests(clickedPoint);
                repaint();
            }
        } else {
            // Second click: confirm move or re-select
            if (clickedPoint == selectedPoint) {
                clearSelection();
            } else if (validDests.contains(clickedPoint)) {
                int from = selectedPoint;
                int to   = clickedPoint;
                clearSelection();
                if (moveCallback != null) moveCallback.accept(from, to);
            } else {
                // Re-select
                selectedPoint = clickedPoint;
                validDests    = computeValidDests(clickedPoint);
                repaint();
            }
        }
    }

    /** Compute where the selected checker can legally go (client-side hint only). */
    private List<Integer> computeValidDests(int from) {
        List<Integer> dests = new ArrayList<>();
        int player = state.getMyPlayerNumber();
        List<Integer> ml   = state.getMovesLeft();
        if (ml.isEmpty()) return dests;

        for (int die : new HashSet<>(ml)) {
            int to = (player == 1) ? from - die : from + die;
            if (to >= 0 && to <= 23) {
                // Check not blocked
                if (state.ownerAt(to) != (3 - player) || Math.abs(state.getBoard()[to]) <= 1) {
                    dests.add(to);
                }
            }
            // Bear-off
            int bearOff = state.getBearOffIndex(player);
            if (to < 0 || to > 23) dests.add(bearOff);
        }
        return dests;
    }

    /** Map pixel coords to a board point index (0-23, or bar indices). */
    private int pointAt(int mx, int my) {
        // Check bar
        if (mx >= barX && mx <= barX + barW) {
            return state.getBarIndex(state.getMyPlayerNumber());
        }
        // Check 24 triangles
        for (int col = 0; col < 12; col++) {
            int x = getPointX(col);
            if (mx >= x && mx < x + pointW) {
                if (my >= boardY && my < boardY + boardH / 2) {
                    return getPointIndex(col, true);
                } else {
                    return getPointIndex(col, false);
                }
            }
        }
        // Bear-off tray area (right side)
        int tx = boardX + boardW + 5;
        if (mx >= tx && mx < tx + 55) {
            return state.getBearOffIndex(state.getMyPlayerNumber());
        }
        return -1;
    }

    public void clearSelection() {
        selectedPoint = -1;
        validDests.clear();
        repaint();
    }

    // ---------------------------------------------------------------
    // Geometry helpers
    // ---------------------------------------------------------------

    /**
     * Returns the board array index for column col.
     * top=true  → points 13-24 (indices 12-23, left to right)
     * top=false → points 12-1  (indices 11-0,  left to right)
     */
    private int getPointIndex(int col, boolean top) {
        if (top) {
            // columns 0-5 = points 13-18 (indices 12-17), shifted past bar
            // columns 6-11 = points 19-24 (indices 18-23)
            return 12 + (col < 6 ? col : col);  // straight map 12-23
        } else {
            // bottom: col 0 = point 12 (index 11), col 11 = point 1 (index 0)
            return 11 - col;
        }
    }

    /** X pixel position of the left edge of column col. */
    private int getPointX(int col) {
        // Insert bar gap after column 5
        int adjCol = col < 6 ? col : col + 1;
        return boardX + adjCol * pointW;
    }

    private int colForBoardIndex(int idx) {
        if (idx >= 12 && idx <= 23) return idx - 12;   // top row
        return 11 - idx;                                 // bottom row
    }

    private int boardIndexForColRow(int idx) { return idx; }
}


    

