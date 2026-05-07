/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame; // FIX #1 – was `package client`, must match every other class in the project

import javax.swing.SwingUtilities;
import com.mycompany.backgammongame.StartScreen; // FIX #2 – StartScreen was unresolvable due to wrong package; explicit import added for clarity

/**
 * Main – application entry point.
 * Launches the start screen on the Swing Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        // Always create and show Swing components on the EDT
        SwingUtilities.invokeLater(() -> {
            StartScreen startScreen = new StartScreen();
            startScreen.setVisible(true);
        });
    }
}

