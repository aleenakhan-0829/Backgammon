/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Aleena's PC
 */
public class StartScreen extends JFrame {

    // ---- UI colours (warm classic board-game palette) ----
    private static final Color BG_DARK   = new Color(30, 18, 10);
    private static final Color BG_MID    = new Color(60, 35, 15);
    private static final Color CREAM     = new Color(245, 230, 195);
    private static final Color GOLD      = new Color(212, 160, 50);
    private static final Color RED_DARK  = new Color(160, 40,  30);

    private JTextField ipField;
    private JButton connectButton;

    public StartScreen() {
        setTitle("Backgammon – Connect");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        // Custom painted background panel
        JPanel bg = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintBackground(g);
            }
        };
        bg.setLayout(new GridBagLayout());
        bg.setBackground(BG_DARK);
        setContentPane(bg);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.gridx  = 0;

        // ---- Title ----
        JLabel title = new JLabel("BACKGAMMON", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 48));
        title.setForeground(GOLD);
        gbc.gridy = 0;
        gbc.insets = new Insets(30, 20, 4, 20);
        bg.add(title, gbc);

        // Sub-title
        JLabel sub = new JLabel("Multiplayer Network Edition", SwingConstants.CENTER);
        sub.setFont(new Font("Serif", Font.ITALIC, 16));
        sub.setForeground(CREAM);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 20, 30, 20);
        bg.add(sub, gbc);

        // Decorative divider
        JSeparator sep = new JSeparator();
        sep.setForeground(GOLD);
        sep.setBackground(GOLD);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 40, 20, 40);
        bg.add(sep, gbc);

        // ---- Server IP label ----
        JLabel ipLabel = new JLabel("Server IP Address:");
        ipLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        ipLabel.setForeground(CREAM);
        gbc.gridy = 3;
        gbc.insets = new Insets(6, 60, 2, 60);
        bg.add(ipLabel, gbc);

        // ---- IP input field ----
        ipField = new JTextField("127.0.0.1");
        ipField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        ipField.setForeground(BG_DARK);
        ipField.setBackground(CREAM);
        ipField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 2),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        ipField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridy = 4;
        bg.add(ipField, gbc);

        // ---- Connect Button ----
        connectButton = new JButton("CONNECT & PLAY") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(RED_DARK.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(RED_DARK.brighter());
                } else {
                    g2.setColor(RED_DARK);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(GOLD);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        connectButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        connectButton.setPreferredSize(new Dimension(250, 48));
        connectButton.setBorderPainted(false);
        connectButton.setContentAreaFilled(false);
        connectButton.setFocusPainted(false);
        connectButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        connectButton.addActionListener(e -> onConnect());

        // Also connect on Enter key in IP field
        ipField.addActionListener(e -> onConnect());

        gbc.gridy = 5;
        gbc.insets = new Insets(20, 80, 10, 80);
        bg.add(connectButton, gbc);

        // ---- Rules hint ----
        JLabel hint = new JLabel(
                "<html><center><font color='#b0a080'>Waiting for 2 players to connect.<br>" +
                "First to bear off all 15 checkers wins!</font></center></html>",
                SwingConstants.CENTER);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 20, 10, 20);
        bg.add(hint, gbc);

        // ---- Exit button ----
        JButton exit = new JButton("Exit");
        exit.setFont(new Font("SansSerif", Font.PLAIN, 12));
        exit.setForeground(CREAM);
        exit.setBackground(BG_MID);
        exit.setBorder(BorderFactory.createLineBorder(BG_MID));
        exit.setFocusPainted(false);
        exit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exit.addActionListener(e -> System.exit(0));
        gbc.gridy = 7;
        gbc.insets = new Insets(4, 180, 20, 180);
        bg.add(exit, gbc);
    }

    /** Paint a simple decorative board-checker pattern in the background. */
    private void paintBackground(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw faint triangle shapes evocative of a backgammon board
        g2.setColor(new Color(50, 30, 15, 80));
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < 6; i++) {
            int x = i * (w / 6);
            int[] xs = {x, x + w / 12, x + w / 6};
            int[] ys = {h, 0, h};
            g2.fillPolygon(xs, ys, 3);
        }
        g2.setColor(new Color(100, 55, 20, 60));
        for (int i = 0; i < 6; i++) {
            int x = i * (w / 6) + w / 12;
            int[] xs = {x, x + w / 12, x + w / 6};
            int[] ys = {h, 0, h};
            g2.fillPolygon(xs, ys, 3);
        }
    }

    /** Called when the user clicks Connect. */
    private void onConnect() {
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid server IP address.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        connectButton.setEnabled(false);
        connectButton.setText("Connecting...");

        // Connect in a background thread so EDT isn't blocked
        new Thread(() -> {
            try {
                NetworkClient net = new NetworkClient(ip, 5555);
                SwingUtilities.invokeLater(() -> {
                    GameWindow gameWindow = new GameWindow(net);
                    gameWindow.setVisible(true);
                    dispose(); // close start screen
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Could not connect to server:\n" + ex.getMessage(),
                            "Connection Failed", JOptionPane.ERROR_MESSAGE);
                    connectButton.setEnabled(true);
                    connectButton.setText("CONNECT & PLAY");
                });
            }
        }).start();
    }
}


