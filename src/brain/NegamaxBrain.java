package brain;

import core.*;
import search.SearchProgress;
import search.Logger;
import java.util.*;

public class NegamaxBrain implements Brain {
    private static final int INF = 1000000;
    
    // Piece values for evaluation
    private static final int P_VAL = 100;
    private static final int N_VAL = 320;
    private static final int B_VAL = 330;
    private static final int R_VAL = 500;
    private static final int Q_VAL = 900;
    private static final int K_VAL = 20000;

    // Piece-Square Tables (PST) representing positional bonuses
    private static final int[] PAWN_PST = {
          0,  0,  0,  0,  0,  0,  0,  0,
         50, 50, 50, 50, 50, 50, 50, 50,
         10, 10, 20, 30, 30, 20, 10, 10,
          5,  5, 10, 25, 25, 10,  5,  5,
          0,  0,  0, 20, 20,  0,  0,  0,
          5, -5,-10,  0,  0,-10, -5,  5,
          5, 10, 10,-20,-20, 10, 10,  5,
          0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_PST = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_PST = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_PST = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_PST = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_PST = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    // Asynchronous progress tracking fields
    private volatile boolean stopSearch = false;
    private volatile int currentDepth = 0;
    private volatile int completedMoves = 0;
    private volatile int totalMoves = 0;
    private volatile boolean isStable = true;

    private Move bestMove = null;
    private int bestScore = -INF;
    private int targetDepth = 100;
    private int totalNodes = 0;

    @Override
    public void init(Board board, long allocatedTimeMs, int targetDepth, long targetNodes, boolean isInfinite) {
        this.stopSearch = false;
        this.currentDepth = 0;
        this.completedMoves = 0;
        this.totalMoves = 0;
        this.isStable = true;
        this.bestMove = null;
        this.bestScore = -INF;
        this.totalNodes = 0;

        if (targetDepth > 0) {
            this.targetDepth = targetDepth; // Fixed depth from GUI
        } else {
            this.targetDepth = 100; // Search as deep as time allows
        }
    }

    @Override
    public Move think(Board board) {
        List<Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) return null;

        // Sort moves so captures/attacks are searched first
        moves.sort((m1, m2) -> Integer.compare(getMoveValue(m2), getMoveValue(m1)));
        this.totalMoves = moves.size();

        // Iterative Deepening
        for (int depth = 1; depth <= targetDepth; depth++) {
            this.currentDepth = depth;
            this.completedMoves = 0;
            long layerStart = System.currentTimeMillis();

            int alpha = -INF;
            int beta = INF;
            Move currentBestMove = null;
            int currentBestScore = -INF;

            for (Move move : moves) {
                board.makeMove(move);
                int score = -negamax(board, depth - 1, 1, -beta, -alpha);
                board.unmakeMove(move);

                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }
                if (score > alpha) {
                    alpha = score;
                }
                this.completedMoves++;
            }
            
            if (currentBestMove != null) {
                // Determine move stability
                if (bestMove != null && !currentBestMove.toUciString().equals(bestMove.toUciString())) {
                    this.isStable = false;
                } else {
                    this.isStable = true;
                }
                bestMove = currentBestMove;
                bestScore = currentBestScore;

                long layerTime = System.currentTimeMillis() - layerStart;

                String scoreString;
                if (Math.abs(bestScore) > 900000) {
                    int plies = INF - Math.abs(bestScore);
                    int mateMoves = (plies + 1) / 2;
                    scoreString = "mate " + (bestScore > 0 ? mateMoves : -mateMoves);
                } else {
                    scoreString = "cp " + bestScore;
                }
                Logger.log("info depth " + depth + " score " + scoreString + " time " + layerTime + " nodes " + totalNodes);
            }
        }

        return bestMove;
    }

    @Override
    public Move getBestMove() {
        return bestMove;
    }

    @Override
    public SearchProgress getProgress() {
        double percent = (totalMoves > 0) ? ((double) completedMoves / totalMoves) : 0.0;
        return new SearchProgress(currentDepth, percent, isStable, bestScore, bestMove);
    }

    @Override
    public void stop() {
        this.stopSearch = true;
    }

    public boolean isStopped() {
        return stopSearch;
    }

    private int negamax(Board board, int depth, int ply, int alpha, int beta) {
        totalNodes++;
        if (board.isDraw()) return 0;
        if (depth == 0) return quiescence(board, alpha, beta);

        List<Move> moves = board.getPseudoLegalMoves();
        moves.sort((m1, m2) -> Integer.compare(getMoveValue(m2), getMoveValue(m1)));

        int legalCount = 0;
        for (Move move : moves) {
            board.makeMove(move);
            if (board.isKingInCheck(board.getSideToMove().opposite())) {
                board.unmakeMove(move);
                continue;
            }
            legalCount++;
            int score = -negamax(board, depth - 1, ply + 1, -beta, -alpha);
            board.unmakeMove(move);

            if (score >= beta) {
                return beta; // Cutoff
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        if (legalCount == 0) {
            if (board.isKingInCheck(board.getSideToMove())) {
                return -INF + ply; // Checkmate (reward shortest mate)
            }
            return 0; // Stalemate
        }

        return alpha;
    }

    private int quiescence(Board board, int alpha, int beta) {
        totalNodes++;
        int standPat = evaluate(board);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        List<Move> moves = board.getPseudoLegalMoves();
        moves.sort((m1, m2) -> Integer.compare(getMoveValue(m2), getMoveValue(m1)));

        for (Move move : moves) {
            if (!move.isCapture()) continue;

            board.makeMove(move);
            if (board.isKingInCheck(board.getSideToMove().opposite())) {
                board.unmakeMove(move);
                continue;
            }
            int score = -quiescence(board, -beta, -alpha);
            board.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    private int getMoveValue(Move move) {
        int val = 0;
        if (move.isCapture()) {
            val += 1000 + (move.getCaptured().getValue() - move.getPiece().getValue() / 10);
        }
        if (move.isPromotion()) {
            val += 900 + move.getPromotion().getValue();
        }
        return val;
    }

    public int evaluate(Board board) {
        Color us = board.getSideToMove();
        Color them = us.opposite();

        int score = 0;
        score += evaluateForColor(board, us) - evaluateForColor(board, them);
        return score;
    }

    private int evaluateForColor(Board board, Color color) {
        int score = 0;

        // Material & Position (PSTs)
        score += evaluatePiece(board, PieceType.PAWN, color, PAWN_PST, P_VAL);
        score += evaluatePiece(board, PieceType.KNIGHT, color, KNIGHT_PST, N_VAL);
        score += evaluatePiece(board, PieceType.BISHOP, color, BISHOP_PST, B_VAL);
        score += evaluatePiece(board, PieceType.ROOK, color, ROOK_PST, R_VAL);
        score += evaluatePiece(board, PieceType.QUEEN, color, QUEEN_PST, Q_VAL);
        score += evaluatePiece(board, PieceType.KING, color, KING_PST, K_VAL);

        return score;
    }

    private int evaluatePiece(Board board, PieceType type, Color color, int[] pst, int baseValue) {
        int score = 0;
        long bb = board.getBitboard(type, color);
        while (bb != 0) {
            int sq = Long.numberOfTrailingZeros(bb);
            score += baseValue;
            // Flip rank for black pieces so same PST works
            int pstIndex = (color == Color.WHITE) ? sq : (sq ^ 56);
            score += pst[pstIndex];
            bb &= bb - 1;
        }
        return score;
    }
}
