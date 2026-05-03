/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

import javax.swing.*;

/**
 *
 * @author Aleena's PC
 */
public class Main {
   

    public static void main(String[] args) {
        // Always update Swing components on the EDT
        SwingUtilities.invokeLater(() -> {
            StartScreen startScreen = new StartScreen();
            startScreen.setVisible(true);
        });
    }
}

