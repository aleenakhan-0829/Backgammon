/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

 package com.mycompany.backgammongame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/**
 *
 * @author Aleena's PC
 */
/**
 * EndScreen - Shown when the game ends.
 * Displays win/loss message and offers Replay or Exit buttons.
 */
public class EndScreen extends JDialog {

    private static final Color BG_DARK  = new Color(20, 12, 6);
    private static final Color GOLD     = new Color(212, 160, 50);
    private static final Color CREAM    = new Color(245, 230, 195);
    private static final Color GREEN    = new Color(50, 180, 70);
    private static final Color RED      = new Color(180, 40, 30);

    public EndScreen(JFrame parent, boolean iWon, Runnable onReplay, Runnable onExit) {
        super(parent, "Game Over", true);
        setSize(420, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new GridBagLayout());
        setContentPane(panel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.gridx  = 0;

        // ---- Result label ----
        String resultText = iWon ? "🏆  YOU WIN!" : "💀  YOU LOSE";
        Color  resultClr  = iWon ? GOLD           : RED;

        JLabel result = new JLabel(resultText, SwingConstants.CENTER);
        result.setFont(new Font("Serif", Font.BOLD, 38));
        result.setForeground(resultClr);
        gbc.gridy = 0;
        panel.add(result, gbc);

        // ---- Message ----
        String msg = iWon
                ? "Congratulations! You bore off all your checkers first."
                : "Better luck next time! Your opponent won this round.";
        JLabel message = new JLabel(
                "<html><center><font color='#c8b880'>" + msg + "</font></center></html>",
                SwingConstants.CENTER);
        message.setFont(new Font("SansSerif", Font.PLAIN, 13));
        gbc.gridy = 1;
        panel.add(message, gbc);

        // ---- Buttons ----
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnRow.setOpaque(false);

        JButton replayBtn = styledBtn("Play Again", GREEN);
        JButton exitBtn   = styledBtn("Main Menu", RED);

        replayBtn.addActionListener(e -> {
            dispose();
            onReplay.run();
        });
        exitBtn.addActionListener(e -> {
            dispose();
            onExit.run();
        });

        btnRow.add(replayBtn);
        btnRow.add(exitBtn);
        gbc.gridy = 2;
        panel.add(btnRow, gbc);
    }

    private JButton styledBtn(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
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
        b.setPreferredSize(new Dimension(130, 42));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}


