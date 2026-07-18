package brain;

import core.*;
import java.util.*;

public class Search {
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

    public Move findBestMove(Board board, int maxDepth) {
        Move bestMove = null;
        int bestScore = -INF;

        List<Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) return null;

        // Sort moves so captures/attacks are searched first
        moves.sort((m1, m2) -> Integer.compare(getMoveValue(m2), getMoveValue(m1)));

        // Iterative Deepening
        for (int depth = 1; depth <= maxDepth; depth++) {
            int alpha = -INF;
            int beta = INF;
            Move currentBestMove = null;
            int currentBestScore = -INF;

            for (Move move : moves) {
                board.makeMove(move);
                int score = -negamax(board, depth - 1, -beta, -alpha);
                board.unmakeMove(move);

                if (score > currentBestScore) {
                    currentBestScore = score;
                    currentBestMove = move;
                }
                if (score > alpha) {
                    alpha = score;
                }
            }
            
            if (currentBestMove != null) {
                bestMove = currentBestMove;
                bestScore = currentBestScore;
            }
        }

        return bestMove;
    }

    private int negamax(Board board, int depth, int alpha, int beta) {
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
            int score = -negamax(board, depth - 1, -beta, -alpha);
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
                return -INF + depth; // Checkmate
            }
            return 0; // Stalemate
        }

        return alpha;
    }

    private int quiescence(Board board, int alpha, int beta) {
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
