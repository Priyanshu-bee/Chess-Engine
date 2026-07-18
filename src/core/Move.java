package core;

public class Move {
    private final Square from;
    private final Square to;
    private final PieceType piece;
    private final PieceType captured;
    private final PieceType promotion;
    
    // Flags: 1 = Double Pawn Push, 2 = En Passant, 4 = Castling
    private final int flags;

    public static final int FLAG_DOUBLE_PAWN_PUSH = 1;
    public static final int FLAG_EN_PASSANT = 2;
    public static final int FLAG_CASTLING = 4;

    public Move(Square from, Square to, PieceType piece) {
        this(from, to, piece, null, null, 0);
    }

    public Move(Square from, Square to, PieceType piece, PieceType captured) {
        this(from, to, piece, captured, null, 0);
    }

    public Move(Square from, Square to, PieceType piece, PieceType captured, PieceType promotion, int flags) {
        this.from = from;
        this.to = to;
        this.piece = piece;
        this.captured = captured;
        this.promotion = promotion;
        this.flags = flags;
    }

    public Square getFrom() {
        return from;
    }

    public Square getTo() {
        return to;
    }

    public PieceType getPiece() {
        return piece;
    }

    public PieceType getCaptured() {
        return captured;
    }

    public PieceType getPromotion() {
        return promotion;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isCapture() {
        return captured != null;
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public boolean isDoublePawnPush() {
        return (flags & FLAG_DOUBLE_PAWN_PUSH) != 0;
    }

    public boolean isEnPassant() {
        return (flags & FLAG_EN_PASSANT) != 0;
    }

    public boolean isCastling() {
        return (flags & FLAG_CASTLING) != 0;
    }

    public String toUciString() {
        String pStr = promotion != null ? String.valueOf(promotion.getSymbol()) : "";
        return from.toString() + to.toString() + pStr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return from == move.from &&
               to == move.to &&
               promotion == move.promotion;
    }

    @Override
    public int hashCode() {
        int result = from.hashCode();
        result = 31 * result + to.hashCode();
        result = 31 * result + (promotion != null ? promotion.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return toUciString();
    }
}
