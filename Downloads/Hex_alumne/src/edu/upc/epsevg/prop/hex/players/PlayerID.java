package edu.upc.epsevg.prop.hex.players;

import edu.upc.epsevg.prop.hex.HexGameStatus;
import edu.upc.epsevg.prop.hex.IAuto;
import edu.upc.epsevg.prop.hex.IPlayer;
import edu.upc.epsevg.prop.hex.PlayerMove;
import edu.upc.epsevg.prop.hex.PlayerType;
import edu.upc.epsevg.prop.hex.SearchType;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que implementa un jugador per al joc HEX.
 * Aquest jugador utilitza l'algoritme Minimax amb poda alpha-beta i una heurística avançada,
 * i la cerca es realitza utilitzant profunditat iterativa, tallant-se pel temps en qualsevol moment.
 */
public class PlayerID implements IPlayer, IAuto {

    private PlayerType playerType;
    private long timeout;
    private long startTime;
    private long exploredNodes;

    /**
     * Constructor de la classe PlayerID. El timeout es gestiona automàticament pel sistema de joc.
     */
    public PlayerID() {
        this.timeout = -1; // El timeout es gestiona per la interfície del joc
    }

    /**
     * Decideix el millor moviment utilitzant l'algoritme Minimax amb poda alpha-beta,
     * i profunditat iterativa.
     *
     * @param state Estat actual del tauler.
     * @return Moviment seleccionat com a millor opció.
     */
    @Override
    public PlayerMove move(HexGameStatus state) {
        if (playerType == null) {
            playerType = state.getCurrentPlayer();
        }

        exploredNodes = 0;
        startTime = System.nanoTime();
        PlayerMove bestMove = null;
        int currentDepth = 1;

        // La cerca iterativa s'executarà fins que el temps es compleixi
        while (true) {
            PlayerMove move = minimaxRoot(state, currentDepth);
            if (move.getPoint() != null) {
                bestMove = move;
            }

            // Comprovem si hem superat el límit de temps
            if (System.nanoTime() - startTime >= timeout * 1_000_000) {
                break; // Si el temps s'ha esgotat, sortim del bucle
            }

            currentDepth++; // Augmentem la profunditat per a la següent iteració
        }

        if (bestMove == null || bestMove.getPoint() == null) {
            System.err.println("No valid move found!");
        }

        return new PlayerMove(
                bestMove.getPoint(),
                exploredNodes,
                currentDepth - 1, // Mostrem la profunditat última trobada abans del timeout
                SearchType.MINIMAX
        );
    }

    /**
     * Notifica que el límit de temps per prendre una decisió s'ha esgotat.
     */
    @Override
    public void timeout() {
        // No fa res, ja que es controla amb el temps que es passa en la cerca
    }

    /**
     * Retorna el nom del jugador.
     *
     * @return Nom del jugador.
     */
    @Override
    public String getName() {
        return "HEXecutionerID";
    }

    /**
     * Implementació de l'arrel del Minimax per trobar el millor moviment possible.
     * La profunditat s'incrementa amb cada iteració.
     *
     * @param state Estat actual del tauler.
     * @param depth Profunditat màxima de la cerca.
     * @return El moviment seleccionat com a millor opció.
     */
    private PlayerMove minimaxRoot(HexGameStatus state, int depth) {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        Point bestMove = null;
        int bestEval = Integer.MIN_VALUE;

        List<ScoredMove> possibleMoves = getPossibleMovesEvaluated(state);

        for (ScoredMove scoredMove : possibleMoves) {
            HexGameStatus newState = new HexGameStatus(state);
            newState.placeStone(scoredMove.move);

            int eval = minimax(newState, depth - 1, alpha, beta, false);

            if (eval > bestEval) {
                bestEval = eval;
                bestMove = scoredMove.move;
            }

            alpha = Math.max(alpha, bestEval);
        }

        return new PlayerMove(bestMove, exploredNodes, depth, SearchType.MINIMAX);
    }

    /**
     * Algoritme Minimax amb poda alpha-beta per trobar el valor heurístic d'un moviment.
     * Utilitza la profunditat iterativa i s'atura quan s'acaba el temps.
     *
     * @param state Estat actual del tauler.
     * @param depth Profunditat màxima de la cerca.
     * @param alpha Valor alfa per a la poda.
     * @param beta Valor beta per a la poda.
     * @param isMaximizing Indica si és el torn del jugador que maximitza.
     * @return Valor heurístic calculat.
     */
    private int minimax(HexGameStatus state, int depth, int alpha, int beta, boolean isMaximizing) {
        exploredNodes++;

        // Si s'ha arribat a la profunditat o el temps ha passat, retornem el valor heurístic
        if (depth == 0 || state.isGameOver() || System.nanoTime() - startTime >= timeout * 1_000_000) {
            return heuristic(state, playerType);
        }

        List<ScoredMove> possibleMoves = getPossibleMovesEvaluated(state);
        int value = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (ScoredMove scoredMove : possibleMoves) {
            HexGameStatus newState = new HexGameStatus(state);
            newState.placeStone(scoredMove.move);

            int eval = minimax(newState, depth - 1, alpha, beta, !isMaximizing);
            if (isMaximizing) {
                value = Math.max(value, eval);
                alpha = Math.max(alpha, value);
            } else {
                value = Math.min(value, eval);
                beta = Math.min(beta, value);
            }

            if (beta <= alpha) break; // Poda alpha-beta
        }

        return value;
    }

    /**
     * Heurística que calcula el valor d'un estat del tauler.
     *
     * @param state Estat actual del tauler.
     * @param playerType Tipus de jugador (PLAYER1 o PLAYER2).
     * @return Valor heurístic calculat.
     */
    private int heuristic(HexGameStatus state, PlayerType playerType) {
        PlayerType opponent = (playerType == PlayerType.PLAYER1) ? PlayerType.PLAYER2 : PlayerType.PLAYER1;

        int centralControl = evaluateCentralControl(state, playerType);
        int distribution = evaluateDistributedPlacement(state, playerType);
        int blocking = evaluateBlockingMoves(state, opponent);

        return centralControl + distribution + blocking;
    }

    /**
     * Avaluació del control central del tauler.
     */
    private int evaluateCentralControl(HexGameStatus state, PlayerType playerType) {
        int score = 0;
        int boardSize = state.getSize();
        int centerX = boardSize / 2;
        int centerY = boardSize / 2;

        final int CENTER_SCORE = 5000;
        final int DECREASE_PER_DISTANCE = 500;

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                int pos = state.getPos(x, y);

                if (pos == playerType.ordinal()) {
                    int distance = Math.max(Math.abs(x - centerX), Math.abs(y - centerY));
                    int stoneScore = CENTER_SCORE - (distance * DECREASE_PER_DISTANCE);

                    if (stoneScore > 0) {
                        score += stoneScore;
                    }
                }
            }
        }

        return score;
    }

    /**
     * Avaluació de la distribució de pedres del jugador.
     */
    private int evaluateDistributedPlacement(HexGameStatus state, PlayerType playerType) {
        int score = 0;
        List<Point> playerStones = getPlayerStones(state, playerType);

        for (Point stone : playerStones) {
            for (Point neighbor : state.getNeigh(stone)) {
                if (state.getPos(neighbor.x, neighbor.y) == 0) {
                    score += 10;
                }
            }
        }

        return score;
    }

    /**
     * Avaluació de moviments de bloqueig per frustrar l'oponent.
     */
    private int evaluateBlockingMoves(HexGameStatus state, PlayerType opponent) {
        int score = 0;
        List<Point> opponentStones = getPlayerStones(state, opponent);

        for (Point stone : opponentStones) {
            for (Point neighbor : state.getNeigh(stone)) {
                if (state.getPos(neighbor.x, neighbor.y) == 0) {
                    score += 15;
                }
            }
        }

        return score;
    }

    /**
     * Obté totes les pedres del jugador al tauler.
     */
    private List<Point> getPlayerStones(HexGameStatus state, PlayerType playerType) {
        List<Point> stones = new ArrayList<>();
        int boardSize = state.getSize();

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                if (state.getPos(x, y) == playerType.ordinal()) {
                    stones.add(new Point(x, y));
                }
            }
        }

        return stones;
    }

    /**
     * Genera i avalua tots els moviments possibles per al jugador.
     */
    private List<ScoredMove> getPossibleMovesEvaluated(HexGameStatus state) {
        List<ScoredMove> scoredMoves = new ArrayList<>();
        int boardSize = state.getSize();

        for (int x = 0; x < boardSize; x++) {
            for (int y = 0; y < boardSize; y++) {
                if (state.getPos(x, y) == 0) {
                    Point move = new Point(x, y);

                    HexGameStatus newState = new HexGameStatus(state);
                    newState.placeStone(move);

                    int heuristicScore = heuristic(newState, playerType);

                    scoredMoves.add(new ScoredMove(move, heuristicScore));
                }
            }
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        return scoredMoves;
    }

    /**
     * Classe interna que representa un moviment avaluat.
     */
    private static class ScoredMove {
        public Point move;
        public int score;

        public ScoredMove(Point move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}
