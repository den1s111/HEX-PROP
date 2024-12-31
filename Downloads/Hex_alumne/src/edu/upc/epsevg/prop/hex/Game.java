package edu.upc.epsevg.prop.hex;

import edu.upc.epsevg.prop.hex.players.HumanPlayer;
import edu.upc.epsevg.prop.hex.players.RandomPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.players.H_E_X_Player;
import edu.upc.epsevg.prop.hex.players.PlayerMinimax;
import edu.upc.epsevg.prop.hex.players.PlayerID;


import javax.swing.SwingUtilities;

/**
 * Checkers: el joc de taula.
 * @author bernat
 */
public class Game {
        /**
     * @param args
     */
    public static void main(String[] args) { 
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //Player 1 wins connecting Left-Right, Player 2 wind connecting Top-Bottom
                
                IPlayer player2 = new H_E_X_Player(2/*GB*/);
                
                IPlayer player1 = new PlayerMinimax(3);
                //IPlayer player1 = new PlayerID();
                //IPlayer player2 = new RandomPlayer("a");
                //IPlayer player2 = new HumanPlayer("b");
                
                new Board(player1 , player2, 11 /*mida*/,  10/*s*/, false);
   
             }
        });
    }
}
