/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
 package client;

 import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author Aleena's PC
 */

public class Gamewindow extends JFrame {

    // ---- Colours ----
    private static final Color BG        = new Color(28, 18, 10);
    private static final Color PANEL_BG  = new Color(45, 28, 12);
    private static final Color GOLD      = new Color(212, 160, 50);
    private static final Color CREAM     = new Color(245, 230, 195);
    private static final Color RED_DARK  = new Color(160, 40, 30);
    private static final Color GREEN     = new Color(50, 160, 70);

    private final NetworkClient net;
    private final clientGameState boardState = new ClientGameState();
    private BoardPanel boardPanel;

    // Controls
    private JButton rollButton;
    private JButton resignButton;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    private JLabel diceLabel;
    private JLabel playerLabel;

    // Game flow flags
    private boolean waitingToRoll = false;
    private boolean waitingToMove = false;

    public Gamewindow(NetworkClient net) {
        this.net = net;
        setTitle("Backgammon");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        // Register message handler — runs on network thread, dispatch to EDT
        net.setMessageListener(msg -> SwingUtilities.invokeLater(() -> handleMessage(msg)));
        // Tell server we are ready
        setStatus("Waiting for second player...");
    }

    // ---------------------------------------------------------------
    // UI Construction
    // ---------------------------------------------------------------

    private void initUI() {
        setLayout(new BorderLayout(6, 6));
        getContentPane().setBackground(BG);

        // ---- Centre: board ----
        boardPanel = new BoardPanel(boardState);
        boardPanel.setMoveCallback(this::onMove);
        add(boardPanel, BorderLayout.CENTER);

        // ---- Right panel: controls + chat ----
        JPanel right = buildRightPanel();
        add(right, BorderLayout.EAST);

        // ---- Bottom: status bar ----
        JPanel bottom = buildStatusBar();
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 8));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 10));
        p.setPreferredSize(new Dimension(190, 0));

        // ---- Player info ----
        playerLabel = styledLabel("Player: ?", 14, GOLD);
        diceLabel   = styledLabel("Dice: -", 13, CREAM);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        infoPanel.setBackground(PANEL_BG);
        infoPanel.add(playerLabel);
        infoPanel.add(diceLabel);

        // ---- Buttons ----
        rollButton   = makeButton("ROLL DICE", GREEN);
        resignButton = makeButton("RESIGN",    RED_DARK);

        rollButton.setEnabled(false);
        rollButton.addActionListener(e -> { net.send("ROLL"); rollButton.setEnabled(false); });
        resignButton.addActionListener(e -> onResign());

        JPanel buttons = new JPanel(new GridLayout(3, 1, 0, 8));
        buttons.setBackground(PANEL_BG);
        buttons.add(rollButton);
        buttons.add(resignButton);

        // ---- Chat area ----
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(20, 12, 5));
        chatArea.setForeground(CREAM);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GOLD), "Chat",
                0, 0, new Font("SansSerif", Font.BOLD, 11), GOLD));
        scroll.setPreferredSize(new Dimension(180, 180));

        chatInput = new JTextField();
        chatInput.setBackground(new Color(35, 22, 8));
        chatInput.setForeground(CREAM);
        chatInput.setCaretColor(CREAM);
        chatInput.setBorder(BorderFactory.createLineBorder(GOLD));
        chatInput.addActionListener(e -> sendChat());

        JPanel chatPanel = new JPanel(new BorderLayout(0, 4));
        chatPanel.setBackground(PANEL_BG);
        chatPanel.add(scroll,    BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        // ---- Assemble ----
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(PANEL_BG);
        top.add(infoPanel, BorderLayout.NORTH);
        top.add(buttons,   BorderLayout.CENTER);

        p.add(top,       BorderLayout.NORTH);
        p.add(chatPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildStatusBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 4));
        p.setBackground(new Color(20, 12, 5));
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD));
        statusLabel = styledLabel("Connecting...", 12, CREAM);
        p.add(statusLabel);
        return p;
    }

    // ---------------------------------------------------------------
    // Message Handling (from server)
    // ---------------------------------------------------------------

    private void handleMessage(String message) {
        String[] parts = message.split(":", 2);
        String cmd  = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "PLAYER_NUM":
                int num = Integer.parseInt(data);
                gameState.setMyPlayerNumber(num);
                playerLabel.setText("You are Player " + num);
                setStatus("Connected as Player " + num + ". Waiting for opponent...");
                net.send("READY");
                break;

            case "START":
                gameState.deserialize(data);
                boardPanel.repaint();
                setStatus("Game started!");
                appendChat("** New game started! **");
                break;

            case "BOARD":
                gameState.deserialize(data);
                boardPanel.repaint();
                break;

            case "DICE":
                String[] dv = data.split(",");
                gameState.setDice(Integer.parseInt(dv[0]), Integer.parseInt(dv[1]));
                diceLabel.setText("Dice: " + dv[0] + " & " + dv[1]);
                boardPanel.repaint();
                break;

            case "YOUR_TURN":
                if ("roll".equals(data)) {
                    waitingToRoll = true;
                    waitingToMove = false;
                    rollButton.setEnabled(true);
                    boardPanel.setMyTurn(false);
                    setStatus("Your turn! Click ROLL DICE.");
                } else if ("move".equals(data)) {
                    waitingToRoll = false;
                    waitingToMove = true;
                    rollButton.setEnabled(false);
                    boardPanel.setMyTurn(true);
                    setStatus("Select a checker to move.");
                }
                break;

            case "WAIT":
                waitingToRoll = false;
                waitingToMove = false;
                rollButton.setEnabled(false);
                boardPanel.setMyTurn(false);
                setStatus("Waiting for opponent...");
                break;

            case "GAMEOVER":
                boardPanel.setMyTurn(false);
                rollButton.setEnabled(false);
                int winner = Integer.parseInt(data);
                boolean iWon = (winner == gameState.getMyPlayerNumber());
                showGameOver(iWon);
                break;

            case "INFO":
                appendChat("[Server] " + data);
                break;

            case "CHAT":
                appendChat(data);
                break;

            case "ERROR":
                setStatus("Error: " + data.replace("_", " "));
                // Re-enable appropriate control
                if ("illegal_move".equals(data)) {
                    boardPanel.clearSelection();
                    if (waitingToMove) boardPanel.setMyTurn(true);
                }
                break;

            case "DISCONNECT":
                JOptionPane.showMessageDialog(this,
                        "Opponent disconnected. Returning to start screen.",
                        "Disconnected", JOptionPane.INFORMATION_MESSAGE);
                returnToStart();
                break;

            default:
                System.out.println("Unknown message: " + message);
        }
    }

    // ---------------------------------------------------------------
    // Player actions
    // ---------------------------------------------------------------

    /** Called by BoardPanel when the user clicks a valid move. */
    private void onMove(int from, int to) {
        net.send("MOVE:" + from + "," + to);
        boardPanel.setMyTurn(false);
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            net.send("CHAT:" + text);
            chatInput.setText("");
        }
    }

    private void onResign() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to resign?",
                "Resign", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            net.send("RESIGN");
        }
    }

    // ---------------------------------------------------------------
    // Game-over / replay
    // ---------------------------------------------------------------

    private void showGameOver(boolean iWon) {
        EndScreen end = new EndScreen(this, iWon, () -> {
            net.send("REPLAY");
            setStatus("Waiting for opponent to agree to replay...");
        }, this::returnToStart);
        end.setVisible(true);
    }

    /** Close this window and go back to the start screen. */
    private void returnToStart() {
        net.close();
        dispose();
        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void setStatus(String msg) {
        statusLabel.setText("  " + msg);
    }

    private void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private JLabel styledLabel(String text, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(color);
        return l;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? (getModel().isRollover() ? bg.brighter() : bg) : bg.darker());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(CREAM);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(160, 40));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}

    
}
