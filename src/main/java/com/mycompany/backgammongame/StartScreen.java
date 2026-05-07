/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * StartScreen - The first screen the player sees.
 * Lets the user enter a server host and port, then connects
 * and launches the GameWindow.
 */
public class StartScreen extends JFrame {

    // ---- Colours (match GameWindow palette) ----
    private static final Color BG       = new Color(28, 18, 10);
    private static final Color PANEL_BG = new Color(45, 28, 12);
    private static final Color GOLD     = new Color(212, 160, 50);
    private static final Color CREAM    = new Color(245, 230, 195);
    private static final Color GREEN    = new Color(50, 160, 70);
    private static final Color RED_DARK = new Color(160, 40, 30);

    private static final int DEFAULT_PORT = 10534; // must match BackgammonGame.SERVER_PORT

    private JTextField hostField;
    private JTextField portField;
    private JButton    connectButton;
    private JLabel     statusLabel;

    public StartScreen() {
        setTitle("Backgammon – Connect");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    // ---------------------------------------------------------------
    // UI Construction
    // ---------------------------------------------------------------

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        // ---- Title ----
        JLabel title = new JLabel("BACKGAMMON", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(28, 0, 10, 0));
        root.add(title, BorderLayout.NORTH);

        // ---- Centre form ----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD, 1),
                BorderFactory.createEmptyBorder(20, 30, 20, 30)));

        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(6, 0, 6, 12);
        lc.gridx  = 0;

        GridBagConstraints fc = new GridBagConstraints();
        fc.fill   = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(6, 0, 6, 0);
        fc.gridx  = 1;
        fc.weightx = 1.0;

        // Host row
        lc.gridy = fc.gridy = 0;
        form.add(styledLabel("Server Host:", 13), lc);
        hostField = styledField("localhost");
        form.add(hostField, fc);

        // Port row
        lc.gridy = fc.gridy = 1;
        form.add(styledLabel("Port:", 13), lc);
        portField = styledField(String.valueOf(DEFAULT_PORT));
        form.add(portField, fc);

        // Connect button row
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridx     = 0;
        bc.gridy     = 2;
        bc.gridwidth = 2;
        bc.fill      = GridBagConstraints.HORIZONTAL;
        bc.insets    = new Insets(16, 0, 0, 0);

        connectButton = makeButton("CONNECT", GREEN);
        connectButton.addActionListener(e -> attemptConnect());
        form.add(connectButton, bc);

        // Wrap form in a padded container
        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setBackground(BG);
        formWrapper.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        formWrapper.add(form, new GridBagConstraints());
        root.add(formWrapper, BorderLayout.CENTER);

        // ---- Status bar ----
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        bottom.setBackground(new Color(20, 12, 5));
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, GOLD));
        statusLabel = styledLabel("Enter server address and connect.", 11);
        bottom.add(statusLabel);
        root.add(bottom, BorderLayout.SOUTH);

        // Allow pressing Enter in either field to trigger connect
        ActionListener enterConnect = e -> attemptConnect();
        hostField.addActionListener(enterConnect);
        portField.addActionListener(enterConnect);
    }

    // ---------------------------------------------------------------
    // Connection logic
    // ---------------------------------------------------------------

    private void attemptConnect() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty()) {
            setStatus("Please enter a server host.", RED_DARK);
            hostField.requestFocus();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            setStatus("Port must be a number between 1 and 65535.", RED_DARK);
            portField.requestFocus();
            return;
        }

        // Disable controls while connecting
        setConnecting(true);
        setStatus("Connecting to " + host + ":" + port + " ...", CREAM);

        final String finalHost = host;
        final int    finalPort = port;

        // Connect on a background thread so the EDT stays responsive
        new Thread(() -> {
            try {
                // Pass a no-op listener here; GameWindow will register the real one
                // immediately in its constructor via net.setMessageListener(...)
                NetworkClient net = new NetworkClient(finalHost, finalPort, msg -> {});

                SwingUtilities.invokeLater(() -> {
                    dispose(); // close start screen
                    GameWindow gw = new GameWindow(net);
                    gw.setVisible(true);
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Could not connect: " + ex.getMessage(), RED_DARK);
                    setConnecting(false);
                });
            }
        }, "Connect-Thread").start();
    }

    /** Toggle controls during a connection attempt. */
    private void setConnecting(boolean connecting) {
        connectButton.setEnabled(!connecting);
        hostField.setEnabled(!connecting);
        portField.setEnabled(!connecting);
    }

    private void setStatus(String msg) {
        setStatus(msg, CREAM);
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private JLabel styledLabel(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setForeground(CREAM);
        return l;
    }

    private JTextField styledField(String defaultValue) {
        JTextField f = new JTextField(defaultValue, 14);
        f.setBackground(new Color(35, 22, 8));
        f.setForeground(CREAM);
        f.setCaretColor(CREAM);
        f.setFont(new Font("Monospaced", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        return f;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                        ? (getModel().isRollover() ? bg.brighter() : bg)
                        : bg.darker());
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
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(200, 42));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
