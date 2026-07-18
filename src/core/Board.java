package core;

import java.util.*;

public class Board {
    // Bitboard array: index maps to PieceType and Color
    // White: 0=PAWN, 1=KNIGHT, 2=BISHOP, 3=ROOK, 4=QUEEN, 5=KING
    // Black: 6=PAWN, 7=KNIGHT, 8=BISHOP, 9=ROOK, 10=QUEEN, 11=KING
    private final long[] bitboards = new long[12];
    
    private long whiteOccupancy;
    private long blackOccupancy;
    private long totalOccupancy;

    private Color sideToMove;
    private int castlingRights; // WK=1, WQ=2, BK=4, BQ=8
    private Square enPassantSquare;
    private int halfmoveClock;

    private final Stack<BoardState> stateHistory = new Stack<>();

    private static class BoardState {
        final int castlingRights;
        final Square enPassantSquare;
        final int halfmoveClock;

        BoardState(int castlingRights, Square enPassantSquare, int halfmoveClock) {
            this.castlingRights = castlingRights;
            this.enPassantSquare = enPassantSquare;
            this.halfmoveClock = halfmoveClock;
        }
    }

    private static final int[] CASTLING_LOOKUP = new int[64];
    static {
        Arrays.fill(CASTLING_LOOKUP, 15);
        CASTLING_LOOKUP[Square.E1.getIndex()] &= ~3;
        CASTLING_LOOKUP[Square.H1.getIndex()] &= ~1;
        CASTLING_LOOKUP[Square.A1.getIndex()] &= ~2;
        CASTLING_LOOKUP[Square.E8.getIndex()] &= ~12;
        CASTLING_LOOKUP[Square.H8.getIndex()] &= ~4;
        CASTLING_LOOKUP[Square.A8.getIndex()] &= ~8;
    }

    public Board() {
        loadFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public void loadFen(String fen) {
        // Clear everything
        Arrays.fill(bitboards, 0L);
        stateHistory.clear();
        
        String[] parts = fen.split("\\s+");
        String placement = parts[0];
        
        // 1. Piece placement
        String[] ranks = placement.split("/");
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r;
            int file = 0;
            for (int c = 0; c < ranks[r].length(); c++) {
                char ch = ranks[r].charAt(c);
                if (Character.isDigit(ch)) {
                    file += Character.getNumericValue(ch);
                } else {
                    Color color = Character.isUpperCase(ch) ? Color.WHITE : Color.BLACK;
                    PieceType type = PieceType.fromSymbol(ch);
                    int sqIndex = rank * 8 + file;
                    int bbIndex = getBbIndex(type, color);
                    bitboards[bbIndex] |= (1L << sqIndex);
                    file++;
                }
            }
        }

        // 2. Active color
        sideToMove = parts[1].equals("w") ? Color.WHITE : Color.BLACK;

        // 3. Castling availability
        castlingRights = 0;
        String castling = parts[2];
        if (castling.contains("K")) castlingRights |= 1;
        if (castling.contains("Q")) castlingRights |= 2;
        if (castling.contains("k")) castlingRights |= 4;
        if (castling.contains("q")) castlingRights |= 8;

        // 4. En passant square
        String ep = parts[3];
        if (ep.equals("-")) {
            enPassantSquare = null;
        } else {
            enPassantSquare = Square.fromName(ep);
        }

        // 5. Halfmove clock
        halfmoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;

        updateOccupancies();
    }

    public Color getSideToMove() {
        return sideToMove;
    }

    public int getCastlingRights() {
        return castlingRights;
    }

    public Square getEnPassantSquare() {
        return enPassantSquare;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public long getBitboard(PieceType type, Color color) {
        return bitboards[getBbIndex(type, color)];
    }

    public long getOccupancy(Color color) {
        return color == Color.WHITE ? whiteOccupancy : blackOccupancy;
    }

    public long getTotalOccupancy() {
        return totalOccupancy;
    }

    private int getBbIndex(PieceType type, Color color) {
        return type.ordinal() + (color == Color.BLACK ? 6 : 0);
    }

    public void updateOccupancies() {
        whiteOccupancy = 0L;
        for (int i = 0; i < 6; i++) {
            whiteOccupancy |= bitboards[i];
        }
        blackOccupancy = 0L;
        for (int i = 6; i < 12; i++) {
            blackOccupancy |= bitboards[i];
        }
        totalOccupancy = whiteOccupancy | blackOccupancy;
    }

    public PieceType getPieceAt(Square sq) {
        long bit = 1L << sq.getIndex();
        for (PieceType type : PieceType.values()) {
            if ((bitboards[getBbIndex(type, Color.WHITE)] & bit) != 0) return type;
            if ((bitboards[getBbIndex(type, Color.BLACK)] & bit) != 0) return type;
        }
        return null;
    }

    public Color getColorAt(Square sq) {
        long bit = 1L << sq.getIndex();
        if ((whiteOccupancy & bit) != 0) return Color.WHITE;
        if ((blackOccupancy & bit) != 0) return Color.BLACK;
        return null;
    }

    public boolean isSquareAttacked(Square sq, Color attackerColor) {
        int sqIdx = sq.getIndex();
        long occupied = totalOccupancy;

        // 1. Attacked by Pawns
        long pawns = bitboards[getBbIndex(PieceType.PAWN, attackerColor)];
        long pawnAttackFromTarget = BitboardUtils.PAWN_ATTACKS[attackerColor.opposite().ordinal()][sqIdx];
        if ((pawnAttackFromTarget & pawns) != 0) return true;

        // 2. Attacked by Knights
        long knights = bitboards[getBbIndex(PieceType.KNIGHT, attackerColor)];
        if ((BitboardUtils.KNIGHT_ATTACKS[sqIdx] & knights) != 0) return true;

        // 3. Attacked by King
        long king = bitboards[getBbIndex(PieceType.KING, attackerColor)];
        if ((BitboardUtils.KING_ATTACKS[sqIdx] & king) != 0) return true;

        // 4. Attacked by Bishop/Queen (Diagonals)
        long bishopsAndQueens = bitboards[getBbIndex(PieceType.BISHOP, attackerColor)] |
                               bitboards[getBbIndex(PieceType.QUEEN, attackerColor)];
        if ((BitboardUtils.getBishopAttacks(sqIdx, occupied) & bishopsAndQueens) != 0) return true;

        // 5. Attacked by Rook/Queen (Straights)
        long rooksAndQueens = bitboards[getBbIndex(PieceType.ROOK, attackerColor)] |
                             bitboards[getBbIndex(PieceType.QUEEN, attackerColor)];
        if ((BitboardUtils.getRookAttacks(sqIdx, occupied) & rooksAndQueens) != 0) return true;

        return false;
    }

    public boolean isKingInCheck(Color color) {
        long kingBb = bitboards[getBbIndex(PieceType.KING, color)];
        if (kingBb == 0) return false;
        int kingSqIdx = Long.numberOfTrailingZeros(kingBb);
        return isSquareAttacked(Square.fromIndex(kingSqIdx), color.opposite());
    }

    public List<Move> getPseudoLegalMoves() {
        List<Move> moves = new ArrayList<>(40);
        Color us = sideToMove;
        Color them = us.opposite();
        long usOccupancy = getOccupancy(us);
        long themOccupancy = getOccupancy(them);

        // 1. Pawn Moves
        long pawns = bitboards[getBbIndex(PieceType.PAWN, us)];
        int pawnShift = (us == Color.WHITE) ? 8 : -8;
        int promoRank = (us == Color.WHITE) ? 7 : 0;
        int startRank = (us == Color.WHITE) ? 1 : 6;

        long tempPawns = pawns;
        while (tempPawns != 0) {
            int fromIdx = Long.numberOfTrailingZeros(tempPawns);
            Square from = Square.fromIndex(fromIdx);
            tempPawns &= tempPawns - 1;

            // Single Push
            int toIdx = fromIdx + pawnShift;
            if (toIdx >= 0 && toIdx < 64 && (totalOccupancy & (1L << toIdx)) == 0) {
                Square to = Square.fromIndex(toIdx);
                if (to.getRank() == promoRank) {
                    addPawnPromotions(moves, from, to, null, 0);
                } else {
                    moves.add(new Move(from, to, PieceType.PAWN));
                    // Double Push
                    int doubleToIdx = fromIdx + 2 * pawnShift;
                    if (from.getRank() == startRank && (totalOccupancy & (1L << doubleToIdx)) == 0) {
                        moves.add(new Move(from, Square.fromIndex(doubleToIdx), PieceType.PAWN, null, null, Move.FLAG_DOUBLE_PAWN_PUSH));
                    }
                }
            }

            // Normal Captures
            long attacks = BitboardUtils.PAWN_ATTACKS[us.ordinal()][fromIdx] & themOccupancy;
            while (attacks != 0) {
                int capIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(capIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                if (to.getRank() == promoRank) {
                    addPawnPromotions(moves, from, to, capPiece, 0);
                } else {
                    moves.add(new Move(from, to, PieceType.PAWN, capPiece));
                }
            }

            // En Passant Capture
            if (enPassantSquare != null) {
                long epBit = 1L << enPassantSquare.getIndex();
                if ((BitboardUtils.PAWN_ATTACKS[us.ordinal()][fromIdx] & epBit) != 0) {
                    moves.add(new Move(from, enPassantSquare, PieceType.PAWN, PieceType.PAWN, null, Move.FLAG_EN_PASSANT));
                }
            }
        }

        // 2. Knight Moves
        long knights = bitboards[getBbIndex(PieceType.KNIGHT, us)];
        while (knights != 0) {
            int fromIdx = Long.numberOfTrailingZeros(knights);
            Square from = Square.fromIndex(fromIdx);
            knights &= knights - 1;

            long attacks = BitboardUtils.KNIGHT_ATTACKS[fromIdx] & ~usOccupancy;
            while (attacks != 0) {
                int toIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(toIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                moves.add(new Move(from, to, PieceType.KNIGHT, capPiece));
            }
        }

        // 3. Bishop Moves
        long bishops = bitboards[getBbIndex(PieceType.BISHOP, us)];
        while (bishops != 0) {
            int fromIdx = Long.numberOfTrailingZeros(bishops);
            Square from = Square.fromIndex(fromIdx);
            bishops &= bishops - 1;

            long attacks = BitboardUtils.getBishopAttacks(fromIdx, totalOccupancy) & ~usOccupancy;
            while (attacks != 0) {
                int toIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(toIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                moves.add(new Move(from, to, PieceType.BISHOP, capPiece));
            }
        }

        // 4. Rook Moves
        long rooks = bitboards[getBbIndex(PieceType.ROOK, us)];
        while (rooks != 0) {
            int fromIdx = Long.numberOfTrailingZeros(rooks);
            Square from = Square.fromIndex(fromIdx);
            rooks &= rooks - 1;

            long attacks = BitboardUtils.getRookAttacks(fromIdx, totalOccupancy) & ~usOccupancy;
            while (attacks != 0) {
                int toIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(toIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                moves.add(new Move(from, to, PieceType.ROOK, capPiece));
            }
        }

        // 5. Queen Moves
        long queens = bitboards[getBbIndex(PieceType.QUEEN, us)];
        while (queens != 0) {
            int fromIdx = Long.numberOfTrailingZeros(queens);
            Square from = Square.fromIndex(fromIdx);
            queens &= queens - 1;

            long attacks = BitboardUtils.getQueenAttacks(fromIdx, totalOccupancy) & ~usOccupancy;
            while (attacks != 0) {
                int toIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(toIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                moves.add(new Move(from, to, PieceType.QUEEN, capPiece));
            }
        }

        // 6. King Moves
        long king = bitboards[getBbIndex(PieceType.KING, us)];
        if (king != 0) {
            int fromIdx = Long.numberOfTrailingZeros(king);
            Square from = Square.fromIndex(fromIdx);

            long attacks = BitboardUtils.KING_ATTACKS[fromIdx] & ~usOccupancy;
            while (attacks != 0) {
                int toIdx = Long.numberOfTrailingZeros(attacks);
                Square to = Square.fromIndex(toIdx);
                PieceType capPiece = getPieceAt(to);
                attacks &= attacks - 1;

                moves.add(new Move(from, to, PieceType.KING, capPiece));
            }

            // Castling
            if (us == Color.WHITE) {
                // White Kingside
                if ((castlingRights & 1) != 0 &&
                    (totalOccupancy & ((1L << Square.F1.getIndex()) | (1L << Square.G1.getIndex()))) == 0 &&
                    !isSquareAttacked(Square.E1, Color.BLACK) &&
                    !isSquareAttacked(Square.F1, Color.BLACK) &&
                    !isSquareAttacked(Square.G1, Color.BLACK)) {
                    moves.add(new Move(Square.E1, Square.G1, PieceType.KING, null, null, Move.FLAG_CASTLING));
                }
                // White Queenside
                if ((castlingRights & 2) != 0 &&
                    (totalOccupancy & ((1L << Square.B1.getIndex()) | (1L << Square.C1.getIndex()) | (1L << Square.D1.getIndex()))) == 0 &&
                    !isSquareAttacked(Square.E1, Color.BLACK) &&
                    !isSquareAttacked(Square.D1, Color.BLACK) &&
                    !isSquareAttacked(Square.C1, Color.BLACK)) {
                    moves.add(new Move(Square.E1, Square.C1, PieceType.KING, null, null, Move.FLAG_CASTLING));
                }
            } else {
                // Black Kingside
                if ((castlingRights & 4) != 0 &&
                    (totalOccupancy & ((1L << Square.F8.getIndex()) | (1L << Square.G8.getIndex()))) == 0 &&
                    !isSquareAttacked(Square.E8, Color.WHITE) &&
                    !isSquareAttacked(Square.F8, Color.WHITE) &&
                    !isSquareAttacked(Square.G8, Color.WHITE)) {
                    moves.add(new Move(Square.E8, Square.G8, PieceType.KING, null, null, Move.FLAG_CASTLING));
                }
                // Black Queenside
                if ((castlingRights & 8) != 0 &&
                    (totalOccupancy & ((1L << Square.B8.getIndex()) | (1L << Square.C8.getIndex()) | (1L << Square.D8.getIndex()))) == 0 &&
                    !isSquareAttacked(Square.E8, Color.WHITE) &&
                    !isSquareAttacked(Square.D8, Color.WHITE) &&
                    !isSquareAttacked(Square.C8, Color.WHITE)) {
                    moves.add(new Move(Square.E8, Square.C8, PieceType.KING, null, null, Move.FLAG_CASTLING));
                }
            }
        }

        return moves;
    }

    private void addPawnPromotions(List<Move> moves, Square from, Square to, PieceType capPiece, int flags) {
        moves.add(new Move(from, to, PieceType.PAWN, capPiece, PieceType.QUEEN, flags));
        moves.add(new Move(from, to, PieceType.PAWN, capPiece, PieceType.ROOK, flags));
        moves.add(new Move(from, to, PieceType.PAWN, capPiece, PieceType.BISHOP, flags));
        moves.add(new Move(from, to, PieceType.PAWN, capPiece, PieceType.KNIGHT, flags));
    }

    public List<Move> getLegalMoves() {
        List<Move> pseudo = getPseudoLegalMoves();
        List<Move> legal = new ArrayList<>(pseudo.size());
        for (Move move : pseudo) {
            makeMove(move);
            if (!isKingInCheck(sideToMove.opposite())) {
                legal.add(move);
            }
            unmakeMove(move);
        }
        return legal;
    }

    public void makeMove(Move move) {
        Color us = sideToMove;
        Color them = us.opposite();

        // 1. Record current state
        stateHistory.push(new BoardState(castlingRights, enPassantSquare, halfmoveClock));

        // Get square indices
        int fromIdx = move.getFrom().getIndex();
        int toIdx = move.getTo().getIndex();
        long fromBit = 1L << fromIdx;
        long toBit = 1L << toIdx;

        // Reset ep square by default
        enPassantSquare = null;

        // Piece to place
        PieceType movingPiece = move.getPiece();
        PieceType placementPiece = move.getPromotion() != null ? move.getPromotion() : movingPiece;

        // 2. Remove moving piece from 'from' square
        bitboards[getBbIndex(movingPiece, us)] &= ~fromBit;

        // 3. Place piece on 'to' square
        bitboards[getBbIndex(placementPiece, us)] |= toBit;

        // 4. Handle captures
        if (move.isCapture()) {
            if (move.isEnPassant()) {
                int epCapIdx = toIdx + (us == Color.WHITE ? -8 : 8);
                bitboards[getBbIndex(PieceType.PAWN, them)] &= ~(1L << epCapIdx);
            } else {
                bitboards[getBbIndex(move.getCaptured(), them)] &= ~toBit;
            }
            halfmoveClock = 0; // reset on capture
        } else if (movingPiece == PieceType.PAWN) {
            halfmoveClock = 0; // reset on pawn push
        } else {
            halfmoveClock++;
        }

        // 5. Handle Double Pawn Push (EP setup)
        if (move.isDoublePawnPush()) {
            int epIdx = fromIdx + (us == Color.WHITE ? 8 : -8);
            enPassantSquare = Square.fromIndex(epIdx);
        }

        // 6. Handle Castling (rook moves)
        if (move.isCastling()) {
            if (us == Color.WHITE) {
                if (toIdx == Square.G1.getIndex()) { // Kingside
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] &= ~(1L << Square.H1.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] |= (1L << Square.F1.getIndex());
                } else if (toIdx == Square.C1.getIndex()) { // Queenside
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] &= ~(1L << Square.A1.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] |= (1L << Square.D1.getIndex());
                }
            } else {
                if (toIdx == Square.G8.getIndex()) { // Kingside
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] &= ~(1L << Square.H8.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] |= (1L << Square.F8.getIndex());
                } else if (toIdx == Square.C8.getIndex()) { // Queenside
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] &= ~(1L << Square.A8.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] |= (1L << Square.D8.getIndex());
                }
            }
        }

        // 7. Update Castling Rights
        castlingRights &= CASTLING_LOOKUP[fromIdx];
        castlingRights &= CASTLING_LOOKUP[toIdx];

        // Swap turn
        sideToMove = them;

        updateOccupancies();
    }

    public void unmakeMove(Move move) {
        Color us = sideToMove.opposite(); // Who gets their move undone
        Color them = sideToMove; // Who made the move

        int fromIdx = move.getFrom().getIndex();
        int toIdx = move.getTo().getIndex();
        long fromBit = 1L << fromIdx;
        long toBit = 1L << toIdx;

        PieceType movingPiece = move.getPiece();
        PieceType placementPiece = move.getPromotion() != null ? move.getPromotion() : movingPiece;

        // 1. Remove placement piece from 'to' square
        bitboards[getBbIndex(placementPiece, us)] &= ~toBit;

        // 2. Put original piece back on 'from' square
        bitboards[getBbIndex(movingPiece, us)] |= fromBit;

        // 3. Restore captured piece
        if (move.isCapture()) {
            if (move.isEnPassant()) {
                int epCapIdx = toIdx + (us == Color.WHITE ? -8 : 8);
                bitboards[getBbIndex(PieceType.PAWN, them)] |= (1L << epCapIdx);
            } else {
                bitboards[getBbIndex(move.getCaptured(), them)] |= toBit;
            }
        }

        // 4. Restore rook if castling
        if (move.isCastling()) {
            if (us == Color.WHITE) {
                if (toIdx == Square.G1.getIndex()) { // Kingside
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] |= (1L << Square.H1.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] &= ~(1L << Square.F1.getIndex());
                } else if (toIdx == Square.C1.getIndex()) { // Queenside
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] |= (1L << Square.A1.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.WHITE)] &= ~(1L << Square.D1.getIndex());
                }
            } else {
                if (toIdx == Square.G8.getIndex()) { // Kingside
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] |= (1L << Square.H8.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] &= ~(1L << Square.F8.getIndex());
                } else if (toIdx == Square.C8.getIndex()) { // Queenside
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] |= (1L << Square.A8.getIndex());
                    bitboards[getBbIndex(PieceType.ROOK, Color.BLACK)] &= ~(1L << Square.D8.getIndex());
                }
            }
        }

        // 5. Restore history state
        BoardState state = stateHistory.pop();
        this.castlingRights = state.castlingRights;
        this.enPassantSquare = state.enPassantSquare;
        this.halfmoveClock = state.halfmoveClock;

        // Swap turn back
        sideToMove = us;

        updateOccupancies();
    }

    public boolean isCheckmate() {
        return isKingInCheck(sideToMove) && getLegalMoves().isEmpty();
    }

    public boolean isStalemate() {
        return !isKingInCheck(sideToMove) && getLegalMoves().isEmpty();
    }

    public boolean isDrawByInsufficientMaterial() {
        int totalPieces = 0;
        int wBishops = 0, bBishops = 0;
        int wKnights = 0, bKnights = 0;
        Square wBishopSq = null, bBishopSq = null;

        for (Square sq : Square.values()) {
            PieceType type = getPieceAt(sq);
            if (type == null) continue;
            Color color = getColorAt(sq);
            
            if (type == PieceType.PAWN || type == PieceType.ROOK || type == PieceType.QUEEN) {
                return false;
            }
            
            totalPieces++;
            if (type == PieceType.BISHOP) {
                if (color == Color.WHITE) {
                    wBishops++;
                    wBishopSq = sq;
                } else {
                    bBishops++;
                    bBishopSq = sq;
                }
            } else if (type == PieceType.KNIGHT) {
                if (color == Color.WHITE) wKnights++;
                else bKnights++;
            }
        }

        if (totalPieces <= 2) return true;
        if (totalPieces == 3 && (wBishops == 1 || bBishops == 1 || wKnights == 1 || bKnights == 1)) return true;
        if (totalPieces == 4 && wBishops == 1 && bBishops == 1) {
            boolean wSqLight = (wBishopSq.getFile() + wBishopSq.getRank()) % 2 != 0;
            boolean bSqLight = (bBishopSq.getFile() + bBishopSq.getRank()) % 2 != 0;
            if (wSqLight == bSqLight) return true;
        }

        return false;
    }

    public boolean isDraw() {
        return isStalemate() || isDrawByInsufficientMaterial() || halfmoveClock >= 100;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("   +-----------------+\n");
        for (int r = 7; r >= 0; r--) {
            sb.append(" ").append(r + 1).append(" | ");
            for (int f = 0; f < 8; f++) {
                Square sq = Square.fromIndex(r * 8 + f);
                PieceType type = getPieceAt(sq);
                if (type == null) {
                    sb.append(". ");
                } else {
                    Color color = getColorAt(sq);
                    char sym = type.getSymbol();
                    if (color == Color.WHITE) sym = Character.toUpperCase(sym);
                    sb.append(sym).append(" ");
                }
            }
            sb.append("|\n");
        }
        sb.append("   +-----------------+\n");
        sb.append("     a b c d e f g h\n");
        sb.append("Turn: ").append(sideToMove).append("\n");
        return sb.toString();
    }
}
