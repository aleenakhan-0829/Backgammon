/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.backgammongame;

/**
 *
 * @author Aleena's PC
 */
public class MoveResult {
    private final boolean valid;
    private final boolean gameOver;

    public MoveResult(boolean valid, boolean gameOver) {
        this.valid    = valid;
        this.gameOver = gameOver;
    }

    public boolean isValid()    { return valid; }
    public boolean isGameOver() { return gameOver; }
}

    

